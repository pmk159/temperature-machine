package bad.robot.temperature.server

import java.net.InetAddress
import java.time.temporal.ChronoUnit.{MINUTES => minutes}
import java.time.{Clock, Instant, ZoneId}

import bad.robot.temperature.rrd.Host
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s.dsl._
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.util.NonEmptyList
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification
import org.specs2.specification.AfterEach

class ConnectionsEndpointTest extends Specification with AfterEach {

  sequential

  "No recent connections" >> {
    val request = Request(GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint.service(fixedClock())
    val response = service(request).unsafePerformSync
    response.as[String].unsafePerformSync must_== "[]"
    response.status must_== Ok
  }

  "After a connection is made" >> {
    ConnectionsEndpoint.update(Host("garage"), Some(xForwardedFor("84.12.43.124")))

    val request = Request(GET, Uri.uri("/connections"))
    val service = ConnectionsEndpoint.service(fixedClock())
    val response = service(request).unsafePerformSync

    response.status must_== Ok
    response.as[String].unsafePerformSync must_==
      """[
        |  {
        |    "host" : {
        |      "name" : "garage"
        |    },
        |    "ip" : {
        |      "value" : "84.12.43.124"
        |    }
        |  }
        |]""".stripMargin
  }

  "Recent connections show up" >> {
    val service = ConnectionsEndpoint.service(fixedClock(Instant.now.plus(4, minutes)))

    val request = Request(GET, Uri.uri("/connections/active/within/5/mins"))
    ConnectionsEndpoint.update(Host("garage"), Some(xForwardedFor("184.14.23.214")))
    val response = service(request).unsafePerformSync

    response.status must_== Ok
    response.as[String].unsafePerformSync must_== """[
                                                    |  {
                                                    |    "host" : {
                                                    |      "name" : "garage"
                                                    |    },
                                                    |    "ip" : {
                                                    |      "value" : "184.14.23.214"
                                                    |    }
                                                    |  }
                                                    |]""".stripMargin
  }

  "Connections expire / only recent connections show up" >> {
    val service = ConnectionsEndpoint.service(fixedClock(Instant.now.plus(6, minutes)))

    val request = Request(GET, Uri.uri("/connections/active/within/5/mins"))
    ConnectionsEndpoint.update(Host("garage"), Some(xForwardedFor("162.34.13.113")))
    val response = service(request).unsafePerformSync

    response.status must_== Ok
    response.as[String].unsafePerformSync must_== "[]"
  }

  def fixedClock(instant: Instant = Instant.now) = Clock.fixed(instant, ZoneId.systemDefault())

  def xForwardedFor(ipAddress: String) = `X-Forwarded-For`(NonEmptyList(Some(InetAddress.getByName(ipAddress))))

  def after = ConnectionsEndpoint.reset()

}