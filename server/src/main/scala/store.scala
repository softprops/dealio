package dealio

object Windows {
  import java.text.SimpleDateFormat
  import java.util.Locale
  import java.util.Date

  private val HourFormat = "yyyy-MM-dd-kk"

  private def inFormat(f: String) =
    new SimpleDateFormat(f, Locale.US).format(
      new Date()
    )

  def hourly = inFormat(HourFormat)
}

case class Key(parts: String*) {
  def flatten = parts.mkString(":")
}

trait Store {
  def apply(k: Key): Option[String]
  def apply(k: Key, v: String, ttl: Option[Int] = None): Unit
  def append(k: Key, v: String): Unit
  def map[T](k: Key)(f: String => T): Seq[T]
  def release: Unit
}

case class RedisStore(uri: String) extends Store {
  import redis.clients.jedis._
  import scala.collection.JavaConversions._

  val (host, port) = uri.split(":") match {
    case Array(host, port) => (host, port.toInt)
  }
  private lazy val underlying = new JedisPool(host, port)

  def db[T](f: Jedis => T): T = {
    val r = underlying.getResource
    try {
      f(r)
    } finally {
      underlying.returnResource(r)
    }
  }

  def apply(k: Key) = Option(db { _.get(k.flatten) })
  
  def apply(k: Key, v: String, ttl: Option[Int] = None) = ttl match {
    case Some(ttl) => ()
    case _ => db { _.set(k.flatten, v) }
  }

  def append(k: Key, v: String) =
    db {
      _.rpush(k.flatten, v)
    }

  def map[T](k: Key)(f: String => T): Seq[T] =
    db {
      _.lrange(k.flatten, 0, -1).toSeq map f
    }

  def release {
    underlying.destroy()
  }
}

object Store extends RedisStore(Props.get("REDISTOGO_URL"))
