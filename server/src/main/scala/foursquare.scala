package dealio

import dispatch._
import dispatch.Http._

import unfiltered._
import unfiltered.request._
import unfiltered.response._

import com.codahale.jerkson.Json._

case class AccessResponse(
  access_token: Option[String],
  error: Option[String]
)

case class User(
  id: String,
  firstName: String,
  lastName: String,
  homeCity: String,
  photo: String,
  gender: String,
  relationship: Option[String]
)

case class Stats(
  checkinsCount: Option[Int],
  usersCount: Option[Int],
  tipCount: Option[Int]
)

case class Special(
  id: String,
  `type`: String,
  message: String,
  description: String,
  finePrint: String,
  unlocked: Boolean,
  icon: String,
  title: String,
  state: String
)

case class Venue(
  id: String,
  name: String,
  contact: String,
  location: String,
  categories: Seq[String],
  verified: Boolean,
  stats: Option[Stats],
  url: Option[String],
  specials: Option[Seq[Special]],
  hereNow: Option[String]
)

case class Location(
  lat: Option[String],
  lng: Option[String],
  name: String
)

case class Event(
  id: String,
  name: String
)

case class Photos(
  count: Int,
  items: Seq[String]
)

case class Comments(
  count: Int,
  items: Seq[String]
)

case class Overlaps(
  count: String,
  items: Seq[String]
)

case class Checkin(
  id: String,
  `type`: String,
  `private`: Option[Boolean],
   user: Option[User],
  timeZone: Option[String],
  venue: Option[Venue],
  location: Option[Location],
  shout: Option[String],
  createdAt: Long,
  source: Option[String],
  event: Option[Event],
  photos: Option[Photos],
  comments: Option[Comments],
  overlaps: Option[Overlaps]
)

trait Presence {
  def present(c: Checkin): Unit
}

case class StorePresence(store: Store) extends Presence {
  def present(c: Checkin) {
    (c.user, c.venue) match {
      case (Some(user), Some(venue)) =>
        store.append(Key(venue.id, Windows.hourly),
          user.id :: user.photo :: Nil mkString(",")
        )
      case _ => println("%s contained no user info" format c)
    }
  }
}

object Foursquare {
  val fs = :/("foursquare.com").secure

  case class Client(id: String, secret: String)

  object Auth {
    val oauth = fs / "oauth2"
    val authenticateUrl = oauth / "authenticate"
    val accessUrl = oauth / "access_token"

    def authenticate(c: Client, ruri: String) =
      authenticateUrl <<? Map(
        "client_id" -> c.id,
        "response_type" -> "code",
        "redirect_uri" -> ruri
      )
    
    def access(c: Client, code: String, ruri: String) = 
      accessUrl << Map(
        "client_id" -> c.id,
        "client_secret" -> c.secret,
        "grant_type" -> "authorization_code",
        "code" -> code,
        "redirect_uri" -> ruri
      )
  }


  object Api {
    import java.text.SimpleDateFormat
    import java.util.{Date, Locale}

    val api = :/("api.foursquare.com").secure / "v2"
    val AccessToken = "oauth_token"
    val VDateFormat = "yyyMMdd"

    def venues(token: String, ll: String) =
      api / "venues" / "managed" <<? Map(
        "v" -> new SimpleDateFormat(VDateFormat, Locale.US).format(
          new Date()
        ),
        AccessToken -> token
      )
  }

  def api(c: Client): Cycle.Intent[Any, Any] = {
    case GET(Path("/fs/venues") & Params(p) & Cookies(cs)) =>
      (cs("fst"), p("ll")) match {
        case (Some(Cookie(_, token, _, _, _, _)), Seq(ll)) =>
          JsonContent ~> ResponseString(
            try { Http(Foursquare.Api.venues(token, ll) as_str) }
            catch {
              case dispatch.StatusCode(400, msg) =>
                println(msg)
                msg
            }
          )
        case _ =>
          JsonContent ~> ResponseString("{}")
      }
  }

  def auth(c: Client): Cycle.Intent[Any, Any] = {
    case r @ Host(host) & GET(Path("/fs/connect") & Params(p)) =>
      Redirect(
        Auth.authenticate(c,
          "%s://%s/fs/auth?%s" format(if(r.isSecure) "https" else "http", host,
                                      p("ll") match {
                                        case Seq(ll) =>
                                          "ll=%s" format ll
                                        case _ => ""
                                      })
        ).to_uri.toString
      )

    case GET(Path("/fs/disconnect")) =>
      ResponseCookies(Cookie("fst", "", path = Some("/"), maxAge = Some(0))) ~>
        Redirect("/")

    case r @ Host(host) & GET(Path("/fs/auth") & Params(p)) =>
      (p("code"), p("error")) match {
        case (Seq(code), _) =>
          println("ruri %s" format r.uri)
          val uri = "%s://%s%s?%s" format(if(r.isSecure) "https" else "http", host, r.uri,
                                         p("ll") match {
                                           case Seq(ll) =>
                                             "ll=%s" format ll
                                           case _ => ""
                                         })
          val ar = parse[AccessResponse](Http(Foursquare.Auth.access(c, code, uri) as_str))
          ar.access_token match {
            case Some(at) =>
              ResponseCookies(Cookie("fst", at, path = Some("/"))) ~> Redirect("/venues")
            case _ =>
              println("fail :/ %s" format ar)
              Redirect("/")
          }
        case _ =>
          println("error? %s" format p)
          Redirect("/")
      }   
  }

  // push end point for https://developer.foursquare.com/docs/responses/checkin.html

  def checkins(pres: Presence): Cycle.Intent[Any, Any] = {
    case POST(Path("/checkins") & Params(p)) =>
      (p("secret"), p("checkin")) match {
        case (Seq(sec), Seq(checkin)) =>
          if(Props.get("FS_PUSH_SEC").equals(sec))
            pres.present(parse[Checkin](checkin))
      }
      Ok
  }
}
