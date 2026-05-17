package chatBot.auth

import java.security.MessageDigest
import chatBot.data._
import chatBot.brain._

/**
 * UserAuth: Manages user authentication and session summaries.
 * 
 * Features:
 * - SHA-256 Password Hashing: Ensures security by not storing plain-text passwords.
 * - Directory Isolation: Each user gets their own folder in 'Users-data'.
 * - Session Summaries: Keeps a quick-access log of past conversation topics.
 */
object UserAuth {

    // The root directory where all user data is persisted
    private val dataRoot = os.pwd / "Users-data"

    /** hashPassword: Converts a plain-text password into a fixed-length SHA-256 string. */
    private def hashPassword(pw: String): String = {
        val md    = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(pw.getBytes("UTF-8"))
        bytes.map("%02x".format(_)).mkString
    }

    /** isHashed: Utility to detect if a stored string is already a SHA-256 hash. */
    private def isHashed(s: String): Boolean = s.length == 64 && s.forall("0123456789abcdef".contains)
 
    // -- Path Helpers (Encapsulated) ---------------------------------------
    private def userDir(name: String):      os.Path = dataRoot / name 
    private def credFile(name: String):     os.Path = dataRoot / name / s"$name.txt" 
    private def summariesFile(name: String):os.Path = dataRoot / name / "Summaries.txt" 
 
    // -- Public API -------------------------------------------------------

    /** Returns true if a directory for this user already exists on disk. */
    def userExists(name: String): Boolean = os.exists(userDir(name))
 
    /** Creates a new user account with hashed password and summary file. */
    def register(name: String, password: String): Boolean = {
        if (os.exists(userDir(name))) {
            false
        } else {
            os.makeDir.all(userDir(name))
            os.write(credFile(name), hashPassword(password)) // Security: Store the hash, not the password
            os.write(summariesFile(name), "") // Initialize summary file
            true
        }
    }
 
    /** Validates user credentials. Supports seamless migration from plain-text to hash. */
    def login(name: String, password: String): Boolean = {
        if (!os.exists(credFile(name))) {
            false
        } else {
            val stored = os.read(credFile(name)).trim
            val hashed = hashPassword(password)
            
            if (stored == hashed) {
                true   // Success: Hash match
            } else if (!isHashed(stored) && stored == password) {
                // Feature: Migrate old plain-text accounts to SHA-256 automatically
                os.write.over(credFile(name), hashed)
                true
            } else {
                false
            }
        }
    }
 
    /** Retrieves session metadata (e.g., "Session #1 - Topics: Italian, Pasta"). */
    def loadSummaries(name: String): List[String] = {
        val f = summariesFile(name)
        if (os.exists(f)) 
            os.read.lines(f).toList.filter(_.nonEmpty) 
        else Nil
    }

    /** Wrapper for loading full interaction history from disk. */
    def loadSessionHistory(user: String, sessionNum: Int): List[String] = {
        val file = userDir(user) / s"chat_$sessionNum.txt"
        if (os.exists(file)) os.read.lines(file).toList else Nil
    }
 
    /** Persists the chat log and updates the user's session summary index. */
    def saveSession(
        name:       String,
        chatNum:    Int,
        history:    List[InteractionEntry],
        topicStr:   String): Unit = {

            // Save the raw interaction log
            val chatFile = userDir(name) / s"chat_$chatNum.txt"
            val chatData = history.map(e => s"${e.userInput}|${e.botResponse}").mkString("\n")
            os.write.over(chatFile, chatData, createFolders = true)

            // Update the index summary
            val newLine = s"Session #$chatNum - Topics: $topicStr | Messages: ${history.size}"
            val f = summariesFile(name)

            val existing = if (os.exists(f)) os.read.lines(f).toList.filter(_.nonEmpty) else Nil
            val updated  = existing.indexWhere(_.startsWith(s"Session #$chatNum")) match {
                case -1  => existing :+ newLine          
                case idx => existing.updated(idx, newLine) 
            }
            os.write.over(f, updated.mkString("\n") + "\n", createFolders = true)
    }
}