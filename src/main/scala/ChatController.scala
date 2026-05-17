package chatBot.controller

import chatBot.ui.Chatbot
import chatBot.auth.UserAuth
import chatBot.brain.*
import chatBot.recommend.*
import chatBot.data.*
import chatBot.engine.*
import chatBot.engine._

case class ChatResult(
  response: String,
  recommendations: List[Recipe]
)

object ChatController {

  // Process user message and return chatbot response + recommendations
  def processMessage(input: String, user: String): ChatResult = {

    val (response, cuisine, showSuggestions) =
      Chatbot.generateResponse(input, user)

    val prefs = PreferenceManager.loadPrefs(user)
    val avoidList = prefs.get("avoid").map(_.split(",").map(_.trim.toLowerCase).toList).getOrElse(Nil)

    // Generate recommendations and filter by avoid list
    val recommendations =
      if (showSuggestions)
        RecommendationEngine.recommend(user, cuisine)
          .filterNot(r => avoidList.exists(a => r.cuisine.equalsIgnoreCase(a) || r.dietaryTags.exists(_.equalsIgnoreCase(a))))
      else
        Nil

    ChatResult(response, recommendations)
  }

  // Load old chat session from storage
  def loadSession(user: String, sessionNum: Int): List[InteractionEntry] = {

    val history = ConversationBrain.loadChat(user, sessionNum)

    // Restore chatbot memory/history
    Chatbot.setHistory(history)

    history
  }

  // Save current session with summarized topics
  def saveSession(user: String, chatNum: Int): Unit = {

    val history = Chatbot.getHistory

    if (history.nonEmpty) {

      // Extract discussed topics from conversation
      val topics = ConversationBrain.extractTopics(history)

      val topicStr =
        if (topics.isEmpty)
          "general chat"
        else
          topics.take(4).mkString(", ")

      UserAuth.saveSession(user, chatNum, history, topicStr)
    }
  }

  // User login
  def login(username: String, password: String): Boolean = UserAuth.login(username, password)

  // Create new account
  def register(username: String, password: String): Boolean = UserAuth.register(username, password)

  // Check if username already exists
  def userExists(username: String): Boolean = UserAuth.userExists(username)

  // Load saved session summaries
  def getSummaries(user: String): List[String] = UserAuth.loadSummaries(user)

  // Load user food preferences
  def getPreferences(user: String): Map[String, String] = PreferenceManager.loadPrefs(user)

  // Read full history file
  def loadHistory(user: String): List[String] = {

    val path = os.pwd / "Users-data" / user / "history.txt"

    if (os.exists(path))
      os.read.lines(path).toList
    else
      List.empty
  }

  // Delete history file
  def clearHistory(user: String): Unit = {

    val path = os.pwd / "Users-data" / user / "history.txt"

    if (os.exists(path))
      os.remove(path)
  }
}