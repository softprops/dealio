package dealio

case class EnvOrProps(resource: String) {
  import java.util.Properties
  lazy val underlying =
    try {
      val p = new Properties()
      p.load(getClass().getResourceAsStream(resource))
      p
    } catch {
      case e => sys.error("failed to load %s" format resource)
    }

  def apply(k: String): Option[String] = Option(System.getenv(k)) match {
    case None => Option(underlying.getProperty(k))
    case value => value
  }

  def get(k: String): String = apply(k).get
}

object Props extends EnvOrProps("/dealio.properties")
