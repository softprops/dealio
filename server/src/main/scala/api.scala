package dealio

import unfiltered._
import unfiltered.request._
import unfiltered.response._

import com.codahale.jerkson.Json._

/*
  user presence is stored as {venue}:{window}:{uid,photo,..?}
  user code is stored as {venue}:{uid}:code:{code}
*/

object Api {
  def at(store: Store): Cycle.Intent[Any, Any] = {
    // who's here?
    case GET(Path(Seg("api" :: "venues" :: venue :: Nil))) =>
      store(Key(venue, Windows.hourly)) match {
        case Some(values) =>
          JsonContent ~> ResponseString("[]")
        case _ =>
          JsonContent ~> ResponseString("[]")
      }
  }

  def codes(store: Store): Cycle.Intent[Any, Any] = {
    // get code for user
    case GET(Path(Seg(
      "api" :: "venues" :: venue :: "codes" :: user :: dim :: Nil)) & Params(p)) =>
      import java.io.OutputStream
      /*store(Key(venue, user, "codes")) match {
        case Some(code) => ResponseString(code)
        case _ => BadRequest
      }*/


      dim.split("x") match {
        case Array(w, h) =>
          val (width, height) = (w.toInt, h.toInt)
          var bars = Bars(user, width, height)_
          ContentType("image/png") ~> new ResponseStreamer {
            def stream(os: OutputStream) {
              bars(os)
            }
          }
        case _ => BadRequest ~>
          ResponseString("expected dimensions in WIDTHxHEIGHT")
      }

    // bind a code to an fs user
    case POST(Path(Seg("api" :: "venues" :: venue :: "codes" :: Nil)) & Params(p)) =>
      (p("user"), p("code")) match {
        case (Seq(user), Seq(code)) =>
          store(Key(venue, user, "codes"), code)
          Created
        case _ => BadRequest ~> ResponseString("user and code are required")
      }
  }
}
