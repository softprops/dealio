package dealio

import unfiltered._
import unfiltered.request._
import unfiltered.response._

import xml._

object Templates {

  def layout(body: NodeSeq)(scripts: String*) = Html(
    <html>
      <head>
        <title>deallio</title>
        <link rel="stylesheet" type="text/css" href="/css/dealio.css"/>
      </head>
      <body>
        <div id="container"> { body } </div>
        <script type="text/javascript" src="/js/jquery.min.js"></script>
        <script type="text/javascript" src="/js/dealio.js"></script>
        { scripts map { s => <script type="text/javascript" src={ "/js/%s.js" format s }></script> } }
      </body>
    </html>)

  val index = layout(
    <div id="index">
      <h1>What the deall.io?</h1>
      <a href="/fs/connect" id="connect" title="connect with foursquare">
       <img src="/img/checkin.png"/>
      </a>
    </div>
  )("index")

  val venues = layout(
    <div>
      <p><a href="/fs/disconnect">log out</a></p>
      <h1>Which venue are you managing?</h1>
      <ul id="venues"></ul>
    </div>
  )("venues")

  def checkins(venue: String) = layout(
    <div>
      <p><a href="/fs/disconnect">log out</a></p>
      <h1>Who's here @ <strong>{ venue }</strong>?</h1>
      <ul id="ci">
      </ul>
    </div>
  )("checkins")

  def code(venue: String, user: String, dim: String = "300x150") = 
    layout(
      <div>
        <img src={"/api/venues/%s/codes/%s/%s" format(venue, user, dim)} />
        <p>{user}</p>
      </div>
    )()

}

object Browser {
  import java.net.URLDecoder.decode

  def pages: Cycle.Intent[Any, Any] = {
    case GET(Path("/") & Cookies(c)) =>
      c("fst") match {
        case Some(Cookie(_, value, _, _, _, _)) =>
          Redirect("/venues")
        case _ =>
          Templates.index
      }
      
    case GET(Path("/venues") & Cookies(c)) =>
      c("fst") match {
        case Some(_) =>
          Templates.venues
        case _ =>
          Redirect("/")
      }
    
    case GET(Path(Seg("checkins" :: at :: venue :: Nil)) & Cookies(c))=>
      c("fst") match {
        case Some(Cookie(_, value, _, _, _, _)) =>
          Templates.checkins(decode(at,"utf-8"))
        case _ =>
          Redirect("/")
      }

    case GET(Path(Seg("venues" :: venue :: "codes" :: user :: Nil))) =>
      Templates.code(venue, user)
  }
}
