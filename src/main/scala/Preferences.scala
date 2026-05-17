package chatBot.engine

/**
 * PreferenceManager: Handles data persistence for user profiles.
 * 
 * This module uses the 'os-lib' library to manage file system operations.
 * It stores user-specific settings (Likes, Dislikes, Avoidance) in text files
 * to ensure that the chatbot 'remembers' the user even after a restart.
 */
object PreferenceManager {

  /** storePref: Appends or updates a preference in the user's profile file. */
  def storePref(user: String, key: String, value: String): Unit = {
    val dir = os.pwd / "Users-data" / user
    if (!os.exists(dir)) os.makeDir.all(dir)
    
    val file = dir / "prefs.txt"
    val current = loadPrefs(user)
    
    // Logic: Append new value if key exists, otherwise create new entry
    val updated = if (key == "avoid" || key == "tag") {
      val existing = current.getOrElse(key, "")
      if (existing.contains(value)) current 
      else current + (key -> (if (existing.isEmpty) value else s"$existing,$value"))
    } else {
      current + (key -> value)
    }

    // Serialize Map to string and write to disk
    val data = updated.map { case (k, v) => s"$k:$v" }.mkString("\n")
    os.write.over(file, data)
  }

  /** loadPrefs: Reads the user's profile file and reconstructs the preference Map. */
  def loadPrefs(user: String): Map[String, String] = {
    val file = os.pwd / "Users-data" / user / "prefs.txt"
    if (os.exists(file)) {
      os.read.lines(file).filter(_.contains(":")).map { line =>
        val parts = line.split(":", 2)
        parts(0) -> parts(1)
      }.toMap
    } else Map.empty
  }

  /** clearPref: Removes a specific key or clears all preferences for a user. */
  def clearPref(user: String, key: String): Unit = {
    val file = os.pwd / "Users-data" / user / "prefs.txt"
    if (os.exists(file)) {
      val current = loadPrefs(user)
      val updated = current - key
      val data = updated.map { case (k, v) => s"$k:$v" }.mkString("\n")
      os.write.over(file, data)
    }
  }
}