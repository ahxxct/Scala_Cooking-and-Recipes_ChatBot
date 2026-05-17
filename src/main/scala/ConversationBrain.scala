package chatBot.brain

import chatBot.data._
import chatBot.auth._
import scala.util.Random

/**
 * ConversationBrain: The Natural Language Processing (NLP) unit.
 * 
 * This module is responsible for analyzing raw strings from the user,
 * extracting metadata (Cuisines, Tags, Moods), and ranking search results.
 */
object ConversationBrain {

  /** detectIntent: Identifies what the user wants to do (Greeting, Search, Cook, etc.) */
  def detectIntent(input: String): String = {
    val low = input.toLowerCase
    if (low.contains("cook") || low.contains("start")) "Cooking"
    else if (low.contains("recommend") || low.contains("suggest")) "Recommendation"
    else if (low.contains("hello") || low.contains("hi")) "Greeting"
    else "Search"
  }

  def isFoodRelated(input: String): Boolean = {
    val low = input.toLowerCase
    val keywords = List("food", "cook", "recipe", "dish", "ingredient", "cuisine", "eat", "meal", "hungry", "chef")
    keywords.exists(low.contains) || 
    RecipeKnowledgeBase.recipes.exists(r => low.contains(r.name.toLowerCase) || low.contains(r.cuisine.toLowerCase))
  }

  def isSuggestionRequest(input: String): Boolean = {
    val low = input.toLowerCase
    low.contains("recommend") || low.contains("suggest") || low.contains("what should i") || low.contains("give me ideas")
  }

  def isPreferenceStatement(input: String): Boolean = {
    val low = input.toLowerCase
    low.contains("i like") || low.contains("i love") || low.contains("prefer") || low.contains("favorite") || 
    low.contains("don't like") || low.contains("avoid") || low.contains("hate")
  }

  def detectCuisine(input: String): Option[String] = {
    RecipeKnowledgeBase.allCuisines.find(c => input.toLowerCase.contains(c.toLowerCase))
  }

  def detectTag(input: String): Option[String] = {
    RecipeKnowledgeBase.allTags.find(t => input.toLowerCase.contains(t.toLowerCase))
  }

  def detectDifficulty(input: String): Option[String] = {
    val low = input.toLowerCase
    if (low.contains("easy") || low.contains("simple")) Some("Easy")
    else if (low.contains("medium") || low.contains("average")) Some("Medium")
    else if (low.contains("hard") || low.contains("complex") || low.contains("expert")) Some("Hard")
    else None
  }

  def detectPrepTime(input: String): Option[Int] = {
    val pattern = "\\b(\\d+)\\s*(min|minute|mins)\\b".r
    pattern.findFirstMatchIn(input).map(_.group(1).toInt)
  }

  def smartRankedSearch(query: String): List[Recipe] = {
    val terms = query.toLowerCase.split("\\W+").filter(_.length > 2)
    if (terms.isEmpty) return Nil

    RecipeKnowledgeBase.recipes
      .map { r =>
        val nameMatch = terms.count(r.name.toLowerCase.contains) * 10
        val cuisineMatch = if (terms.exists(r.cuisine.toLowerCase.contains)) 15 else 0
        val ingredientMatch = terms.count(ing => r.ingredients.exists(_.toLowerCase.contains(ing))) * 5
        val tagMatch = terms.count(tag => r.dietaryTags.exists(_.toLowerCase.contains(tag))) * 8
        
        (r, nameMatch + cuisineMatch + ingredientMatch + tagMatch)
      }
      .filter(_._2 > 0)
      .sortBy(-_._2)
      .map(_._1)
      .take(5)
  }

  def recipesForAndPhrase(input: String): Option[List[Recipe]] = {
    if (!input.contains(" and ")) return None
    val parts = input.split(" and ").map(_.trim)
    if (parts.length < 2) return None

    val listA = smartRankedSearch(parts(0))
    val listB = smartRankedSearch(parts(1))
    
    val intersection = listA.filter(r => listB.exists(_.name == r.name))
    if (intersection.nonEmpty) Some(intersection) 
    else Some((listA ++ listB).distinct.take(5))
  }

  def extractTopics(history: List[InteractionEntry]): List[String] = {
    history.flatMap { entry =>
      val cuisine = detectCuisine(entry.userInput)
      val tag = detectTag(entry.userInput)
      cuisine.toList ++ tag.toList
    }.distinct.take(5)
  }

  def getUserMood(history: List[InteractionEntry]): String = {
    val positive = List("great", "good", "happy", "thanks", "wow", "nice", "love")
    val negative = List("bad", "sad", "angry", "slow", "wrong", "hate", "no")
    
    val text = history.map(_.userInput.toLowerCase).mkString(" ")
    val posCount = positive.count(text.contains)
    val negCount = negative.count(text.contains)
    
    if (posCount > negCount) "Positive"
    else if (negCount > posCount) "Frustrated"
    else "Neutral"
  }

  def detectRepeatedQuery(input: String, history: List[InteractionEntry]): Boolean = {
    history.takeRight(3).exists(_.userInput.toLowerCase == input.toLowerCase)
  }

  def loadChat(user: String, sessionNum: Int): List[InteractionEntry] = {
    UserAuth.loadSessionHistory(user, sessionNum).zipWithIndex.map { case (line, i) =>
      val parts = line.split("\\|")
      if (parts.length >= 2) InteractionEntry(i + 1, parts(0), parts(1), "Legacy")
      else InteractionEntry(i + 1, line, "...", "Unknown")
    }
  }
}