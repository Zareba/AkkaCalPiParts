package main.scala

import scala.math.BigDecimal

case object CalPiShutdown         
//case class CalPiStart(nrOfParts: Int, partSize: Int)
case class CalPiPartsInfo(startPart: Int, nrOfParts: Int)
case class CalPiPartsResult(result: BigDecimal)

object AkkaCalPiParts extends App {
    
    import com.typesafe.config.ConfigFactory
    import akka.actor.{ Props, Actor, ActorRef, ActorSystem }
    import akka.routing.FromConfig
    
    val BigOne = BigDecimal(1)
    val BigTwo = BigDecimal(2)
    val BigFour = BigDecimal(4)
    
    class CalPiPartsWorker(returnResultTo: ActorRef) extends Actor {
        def calPiParts(startPart: Int, nrOfParts: Int): BigDecimal = {
            var i = startPart
            val end = startPart + nrOfParts
            var result = BigDecimal(0)
            while (i < end) {
                result =
                    if (i % 2 == 0)
                        result + BigOne / (BigTwo * BigDecimal(i) + BigOne)
                    else
                        result - BigOne / (BigTwo * BigDecimal(i) + BigOne)
                i += 1
            }
            BigFour * result
        }

        def receive = {
            case CalPiPartsInfo(startPart, nrOfParts) =>
                println("Calculation request received")
                // Calculate the result and send it to the client actor
                returnResultTo ! CalPiPartsResult(calPiParts(startPart, nrOfParts))
        }
    }
	
    // Create actor system named CalPiPartsServer
    val system = ActorSystem("CalPiPartsServer", ConfigFactory.load.getConfig("calPiPartsServer"))
    // Path to client actor name receiveSumCalPiParts 
    val returnResultTo = system.actorFor("akka://CalPiClient@10.0.1.32:2552/user/calPiMaster/calPiPartsReceiveSum")
    // Start CalPiPartsWorker router, named calPiPartsRouter
    system.actorOf(Props(new CalPiPartsWorker(returnResultTo)).withRouter(FromConfig()), "calPiPartsRouter")
    // Start shutdown listener named calPiPartsListener
    val listener = system.actorOf(Props(new Actor {
        def receive = {
            case CalPiShutdown =>
                println("Shutdown")
                // Shutdown the CalPiPartsSystem server actor system
                context.system.shutdown
        }
    }), "calPiPartsListener")

}
