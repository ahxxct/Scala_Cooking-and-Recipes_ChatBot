//> using dep com.lihaoyi::os-lib:0.11.8
import scala.io._
import os._
import scala.util.Random
import chatBot.data._
import chatBot.brain._
import chatBot.auth._
import chatBot.engine._
import chatBot.response._
import chatBot.recommend._
import chatBot.ui._


// <<<< HELPERS >>>>

// Validates that the input is non-empty and contains no whitespace
// BUG FIX (password): password validation now done separately — passwords
// may not have spaces but a general guide entry can have spaces.
def isValid(s: String): Boolean = s.nonEmpty && !s.exists(_.isWhitespace)

// Validates a password — no spaces allowed
def isValidPassword(s: String): Boolean = s.nonEmpty && !s.contains(' ')

// Reads validated (non-empty, no-whitespace) user input and retries until valid
def readValid(prompt: String): String = {
    print(prompt)
    val raw = StdIn.readLine()
    if (raw == null) { println(); "exit" }   // Handle Ctrl-D / EOF safely
    else {
        val input = raw.trim
        if (isValid(input)) input
        else { print("Invalid input — please try again. "); readValid(prompt) }
    }
}

// Reads a password (no spaces required but can have other chars)
def readPassword(prompt: String): String = {
    print(prompt)
    val raw = StdIn.readLine()
    if (raw == null) { println(); "exit" }
    else {
        val input = raw.trim
        if (isValidPassword(input)) input
        else { print("Password cannot contain spaces — please try again. "); readPassword(prompt) }
    }
}


// <<<< MAIN >>>>

@main def startChefBot(): Unit = {

    println(
        """|
           |<==========================================>
           |      WELCOME TO KOKI WOKI CHEFBOT
           |<==========================================>
           |""".stripMargin
    )

    var currentUser = ""

    // ── Authentication loop ───────────────────────────────────────────────────
    while (currentUser.isEmpty) {

        val name = readValid("Username (or 'exit' to quit): ")
        if (name.equalsIgnoreCase("exit")) { println("\nGoodbye! "); return }

        if (UserAuth.userExists(name)) {

            // Existing user login
            val pass = readPassword("Password: ")
            if (pass.equalsIgnoreCase("exit")) { println("\nGoodbye! "); return }

            if (UserAuth.login(name, pass)) currentUser = name
            else println("Incorrect password — try again.\n")

        } else {

            // BUG FIX: original used `val ans = ""` (immutable) and then tried to
            // reassign it inside a while loop. Also the loop condition was inverted.
            // Fixed to use a proper var + a clear loop structure.
            var ans = ""
            while (!List("y", "n", "exit").contains(ans)) {
                val raw = readValid(s"  User '$name' not found. Register? (y/n/exit): ").trim.toLowerCase
                if (List("y", "n", "exit").contains(raw)) ans = raw
                else println("  Please enter 'y', 'n', or 'exit'.")
            }

            ans match {
                case "exit" =>
                    println("\nGoodbye! "); return

                case "y" =>
                    // BUG FIX (requirement): password must have no spaces
                    val pass = readPassword("Create a password (no spaces): ")
                    if (pass.equalsIgnoreCase("exit")) { println("\nGoodbye! "); return }
                    if (UserAuth.register(name, pass)) currentUser = name

                case "n" =>
                    println("  Ok, try a different username.\n")
            }
        }
    }

    println()

    // ── Session resume ────────────────────────────────────────────────────────
    val summaries  = UserAuth.loadSummaries(currentUser)
    var chatNum    = summaries.size + 1
    var loadedSize = 0

    if (summaries.nonEmpty) {

        println(s"--- ${currentUser}'s Cooking History (${summaries.size} session(s)) ---")
        summaries.zipWithIndex.foreach { case (s, i) => println(s"  ${i + 1}. $s") }
        println("---------------------------------------------------")
        print("Enter a session number to resume, or press Enter for a new chat: ")

        val raw    = StdIn.readLine()
        val choice = if (raw == null) "" else raw.trim

        if (choice.nonEmpty) {
            scala.util.Try(choice.toInt).toOption match {

                case Some(n) if n >= 1 && n <= summaries.size =>
                    val loaded = ConversationBrain.loadChat(currentUser, n)
                    if (loaded.nonEmpty) {
                        chatNum    = n
                        Chatbot.setHistory(loaded)
                        loadedSize = loaded.size
                        println(s" Resuming session #$n (${loaded.size} messages loaded).")
                    } else {
                        println(s" Session #$n has no data. Starting a new chat.")
                    }

                case _ =>
                    println(s"  '$choice' is not a valid number. Starting a new chat.")
            }
        }

        println()
    }

    // ── Greeting & stored preferences ─────────────────────────────────────────
    println(ResponseFormatter.greetingResponse(currentUser))

    val prefs = PreferenceManager.loadPrefs(currentUser)
    if (prefs.nonEmpty) {
        val prefStr = prefs.map { case (k, v) => s"$v ($k)" }.mkString(", ")
        println(s"  Remembered preferences: $prefStr")
    }

    println()

    // ── Main chat loop ────────────────────────────────────────────────────────
    var active = true

    while (active) {

        println(s"[$currentUser]: ")
        val raw = StdIn.readLine()

        // BUG FIX: original had no null check — Ctrl-D caused a NullPointerException
        if (raw == null) {
            active = false
        } else {
            val input = raw.trim

            input.toLowerCase match {

                case cmd if cmd == "logout" || cmd == "exit" =>
                    active = false

                case "" =>
                    println("  (Type something, or 'help' for the guide.)\n")

                case _ =>
                    // generateResponse now returns a triple (response, cuisineCtx, showSuggestions)
                    val (resp, ctxCuisine, showSuggestions) =
                        Chatbot.generateResponse(input, currentUser)

                    println(s"\nChefBot:\n$resp")

                    // Chef suggestions — only shown when contextually relevant
                    if (showSuggestions) {
                        val suggestions = RecommendationEngine.recommend(currentUser, ctxCuisine)
                        if (suggestions.nonEmpty) {
                            println("  Chef's Suggestions (based on your taste):")
                            suggestions.foreach(r =>
                                println(s"     -> ${r.name}  [${r.cuisine} - ${r.difficulty} - ${r.prepTime} min]"))
                        }
                    }

                    println()
            }
        }
    }

    // ── Session save ──────────────────────────────────────────────────────────
    val history = Chatbot.getHistory

    if (history.size > loadedSize) {

        val topics   = ConversationBrain.extractTopics(history)
        val topicStr = if (topics.isEmpty) "general chat" else topics.take(4).mkString(", ")

        UserAuth.saveSession(currentUser, chatNum, history, topicStr)

        println(
            s"""|
                |==========================================
                |  Session saved! See you next time, $currentUser
                |  Topics : $topicStr
                |==========================================
                |""".stripMargin
        )

    } else {
        println(s"\nNothing to save. See you next time, $currentUser!")
    }
}