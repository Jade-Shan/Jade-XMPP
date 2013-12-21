object Example extends App {
 
    if (args.length > 1)
        println("Say: " + args(0) + " " + args(1))
    else if (args.length > 0)
        println("Say: " + args(0))
    else
        println("Say nothing")
 
}
