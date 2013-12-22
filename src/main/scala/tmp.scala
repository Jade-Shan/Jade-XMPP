package tmp

import scala.actors.Actor
 
object SillyActor extends Actor {
  def act() {
    for (i <- 1 to 5) {
      println("I'm acting!")
      Thread.sleep(1000)
    }
  }
}


object InputListener extends Actor {

	def act() {
		var keepListening = true
		while (keepListening) {
			val str = Console.readLine
			if ("end" == str) keepListening = false
			else println("you input : " + str)
		}
	}

}

object TestWriter extends Actor {

	def act() {
		var keepWritting = true
		while (keepWritting) {
			receive {
				case msg =>
				println("received message: "+ msg)
			}
		}
	}

}
