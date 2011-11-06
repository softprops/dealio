package dealio

import unfiltered._
import unfiltered.netty._

object Server {
  def main(args: Array[String]) {

    val store: Store = RedisStore(Props.get("REDISTOGO_URL"))
    val fsclient = Foursquare.Client(Props.get("FS_KEY"), Props.get("FS_SEC"))

    Http(Props("PORT").map(_.toInt).getOrElse(8080))
      .resources(getClass().getResource("/www/"))
      .handler(
        cycle.Planify {
          Browser.pages orElse(
            Foursquare.auth(fsclient) orElse Foursquare.checkins(StorePresence(store)) orElse(
              Foursquare.api(fsclient) orElse Api.at(store) orElse Api.codes(store)
            )
          )
        }
      ).beforeStop({ store.release })
       .run
  }
}
