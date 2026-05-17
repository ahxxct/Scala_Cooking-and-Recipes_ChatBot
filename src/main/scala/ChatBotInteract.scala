package chatBot.ui

import scala.util.Random
import chatBot.data._
import chatBot.brain._
import chatBot.auth._
import chatBot.engine._
import chatBot.response._
import chatBot.recommend._

/**
 * Chatbot Object: The core interaction engine for KokiWoki.
 * 
 * This module acts as the "Brain" and "Router", processing natural language input,
 * maintaining conversation state, and orchestrating responses.
 */
object Chatbot {

  // ── State Variables (Encapsulated) ────────────────────────────────────
  private var history: List[InteractionEntry] = Nil // Keeps track of all messages in the current session
  private var currentFlow: Option[String]     = None // Tracks multi-step dialogs (e.g. "quick_meal")
  private var lastCuisineContext: Option[String] = None // Remembers the last cuisine mentioned
  private var lastRecommendations: List[Recipe]  = Nil // Cache for follow-up actions like "more info"
  private var cookingRecipe: Option[Recipe]    = None // The recipe currently being cooked
  private var cookingStep                      = 0    // Current step index in the instructions list

  def setHistory(h: List[InteractionEntry]): Unit = history = h
  def getHistory: List[InteractionEntry]           = history

  /** Full state reset — useful when user logs out / new session starts. */
  def resetState(): Unit = {
    currentFlow        = None
    lastRecommendations = Nil
    cookingRecipe      = None
    cookingStep        = 0
  }

  // ── Main Entry Point ──────────────────────────────────────────────────
  /**
   * Orchestrates the response generation pipeline.
   * Processes input -> Detects Context -> Routes to Handler -> Appends to History.
   */
  def generateResponse(userInput: String, user: String): (String, Option[String], Boolean) = {
    val input = userInput.toLowerCase.trim // Standardize input for rule matching

    // 1. Detect Context & Store Preferences
    val contextCuisine = ConversationBrain.detectCuisine(input)
    val contextTag     = ConversationBrain.detectTag(input)
    val contextDiff    = ConversationBrain.detectDifficulty(input)

    if (contextCuisine.isDefined) lastCuisineContext = contextCuisine
    
    val allIngredientKeywords = List(
      "chicken", "cheese", "beef", "rice", "pasta",
      "tomato", "onion", "garlic", "egg", "bread",
      "fish", "tuna", "shrimp", "cream", "potato"
    )
    val mentionedIngredients = allIngredientKeywords.filter(input.contains)

    val showSuggestions = contextCuisine.isDefined || contextTag.isDefined || mentionedIngredients.nonEmpty

    // 2. Route to Handlers
    val (rawResponse, finalShowSuggestions) = routeInputToHandler(
      input, userInput, user, contextCuisine, contextTag, contextDiff, mentionedIngredients, showSuggestions
    )

    // 3. Finalize Response
    val isRepeated     = ConversationBrain.detectRepeatedQuery(input, history)
    val repeatedPrefix = if (isRepeated) ResponseFormatter.repeatedQueryNote() else ""
    val finalResponse  = repeatedPrefix + rawResponse
    val intent         = ConversationBrain.detectIntent(userInput)

    history = history :+ InteractionEntry(history.size + 1, userInput, finalResponse, intent)

    (finalResponse, contextCuisine, finalShowSuggestions)
  }

  /**
   * Core Rule-Based Engine: Uses logic branching to decide which specialized handler
   * should process the user's input based on detected keywords and conversation state.
   */
  private def routeInputToHandler(
    input: String, userInput: String, user: String,
    ctxCuisine: Option[String], ctxTag: Option[String], ctxDiff: Option[String],
    mentionedIngredients: List[String],
    initialShowSuggestions: Boolean
  ): (String, Boolean) = {
    
    val greetings = List("hi", "hello", "hey", "good morning", "good evening", "how are you", "how are u", "whats up", "whatsapp")
    val thanks    = List("thanks", "thank you", "thx")
    val bye       = List("bye", "goodbye", "see you")

    val firstWord = input.split("\\W+").headOption.getOrElse("")
    val isHelloz = firstWord.matches("^helloz+$")

    val cleanedInput = input.replaceAll("[!?,.]", "")
    val isLoveYou = cleanedInput == "i love you" || cleanedInput == "i love u" || 
                    cleanedInput.matches("^i love you+$") || cleanedInput.matches("^i love u+$")

    // Match priority: Exact commands -> Specific flows -> Generic search
    if (greetings.exists(g => input == g || input.startsWith(g + " ")) || isHelloz) (handleGreeting(user), false)
    else if (isLoveYou) (handleLoveResponse(user), false)
    else if (thanks.exists(input.contains)) (handleThanks(), false)
    else if (bye.exists(input.contains)) (handleBye(user), false)
    else if (input.contains("who are you") || input.contains("your name")) (handleIdentity(), false)
    else if (input == "help" || input.contains("what can you do")) (ResponseFormatter.showGuide(), false)
    else if (input.contains("summary") || input.contains("topics")) (handleSummary(), false)
    else if ((input.contains("not ") || input.contains("no ") || input.contains("don't ") || input.contains("dont ") || input.contains("never ")) && 
             (ctxCuisine.isDefined || ctxTag.isDefined || ctxDiff.isDefined)) {
      val what = ctxCuisine.orElse(ctxTag).orElse(ctxDiff).get
      PreferenceManager.storePref(user, "avoid", what)
      (s"Understood! I'll make sure to avoid $what for you. 👍\n\nIs there something else you'd prefer instead? (e.g. 'I want something with meat')", false)
    }
    else if (ConversationBrain.isPreferenceStatement(input) && (ctxCuisine.isDefined || ctxTag.isDefined || ctxDiff.isDefined)) {
      ctxCuisine.foreach(c => PreferenceManager.storePref(user, "cuisine", c))
      ctxTag.foreach(t     => PreferenceManager.storePref(user, "tag", t))
      ctxDiff.foreach(d    => PreferenceManager.storePref(user, "difficulty", d))
      
      val what = ctxCuisine.orElse(ctxTag).orElse(ctxDiff).getOrElse("that")
      (s"Got it! I'll remember you prefer $what 👍\n\nWant me to suggest some $what recipes? Just say 'recommend'!", true)
    }
    else if (ConversationBrain.recipesForAndPhrase(input).isDefined) (handleAndPhrase(input), true)
    else if (detectTimeSearch(input).isDefined) (handleTimeSearch(input, detectTimeSearch(input).get), true)
    else if (input.contains("hungry") || input.contains("what should i eat")) (handleHungry(), false)
    else if (input.contains("quick") || input.contains("fast") || currentFlow.contains("quick_meal")) (handleQuickMealFlow(input, user), true)
    else if (input.contains("comfort") || currentFlow.contains("comfort_food")) (handleComfortFoodFlow(input, user), true)
    else if (input.contains("healthy") || currentFlow.contains("healthy_food")) (handleHealthyFoodFlow(input, user), true)
    else if (mentionedIngredients.size >= 2) (handleMultipleIngredients(mentionedIngredients), true)
    else if (ConversationBrain.isSuggestionRequest(input) || input.contains("recommend") || input.contains("suggest")) (handleRecommend(user), true)
    else if (input.contains("more") && lastRecommendations.nonEmpty) (handleMore(), true)
    else if ((input.contains("full recipe") || input.contains("how to make this") || input.contains("show recipe")) && lastRecommendations.nonEmpty) {
       (handleKnownRecipe(lastRecommendations.head.name.toLowerCase), true)
    }
    else if (input.contains("similar") && lastRecommendations.nonEmpty) {
       val r = lastRecommendations.head
       (handleDefaultSearch(s"${r.cuisine} ${r.ingredients.head}", "", user), true)
    }
    else if (input.contains("start cooking") || (input.contains("cook") && !input.contains("what to cook"))) (handleStartCooking(input), false)
    else if (input == "done" && cookingRecipe.isDefined) (handleCookingStep(), false)
    else if (input.contains("what cuisine") || input.contains("list cuisine") || input == "cuisine") {
       (s"I have recipes from these cuisines 🌍:\n\n${RecipeKnowledgeBase.allCuisines.map(c => s"• $c").mkString("\n")}\n\nWhich one would you like to explore?", false)
    }
    else if (input.contains("what tag") || input.contains("list tag") || input == "tag") {
       (s"You can filter by these tags 🏷️:\n\n${RecipeKnowledgeBase.allTags.map(t => s"• $t").mkString("\n")}\n\nAny of these catch your eye?", false)
    }
    else (handleDefaultSearch(input, userInput, user), initialShowSuggestions)
  }

  // ── Specialized Handlers ──────────────────────────────────────────────

  def greetUser(): String = handleGreeting("User")

  def handleUserInput(userInput: String, user: String): String = {
    val (resp, _, _) = generateResponse(userInput, user)
    resp
  }

  def parseInput(input: String): List[String] = {
    input.toLowerCase.split("\\W+").toList.filter(_.length > 2)
  }

  private def handleGreeting(user: String): String = {
    currentFlow = None
    Random.shuffle(List(
      s"Hello Chef $user 👨\u200d🍳\n\nWhat would you like to cook today?",
      "I'm doing great, thank you for asking! 😊\nReady to cook something amazing?",
      "Hey there 🍽️\n\nLooking for something delicious?",
      "Hi 👋\n\nI can help with recipes and cooking ideas!",
      "Welcome back Chef 👨\u200d🍳"
    )).head
  }

  private def handleLoveResponse(user: String): String = {
    Random.shuffle(List(
      s"Aww, thank you Chef $user! I love cooking and helping you! 👨‍🍳❤️ Let's make something amazing!",
      "I love you too! You make the kitchen a better place! 👨‍🍳❤️ Ready to cook something delicious?",
      s"That's so sweet, Chef $user! ❤️ What recipe are we whipping up today? 🍳",
      "Aww! I appreciate the love! Happy cooking! 👨‍🍳🍲❤️"
    )).head
  }

  private def handleThanks(): String = {
    Random.shuffle(List("You're welcome 👨\u200d🍳", "Happy cooking 🍽️", "Enjoy your meal 😄")).head
  }

  private def handleBye(user: String): String = {
    currentFlow = None
    Random.shuffle(List(s"Goodbye Chef $user 👋", "See you next time 🍽️", "Keep cooking 👨\u200d🍳🔥")).head
  }

  private def handleIdentity(): String = {
    "I'm Koki Woki 👨\u200d🍳\n\nYour smart cooking assistant!\n\nI can help with:\n• Recipes\n• Healthy meals\n• Quick dishes\n• Cooking guidance\n\nType 'help' to see all commands!"
  }

  def summarizeConversation(history: List[InteractionEntry]): String = {
    val topics = ConversationBrain.extractTopics(history)
    val mood   = ConversationBrain.getUserMood(history)
    
    if (topics.isEmpty) s"We've just started our culinary journey! You seem to be in a $mood mood."
    else s"We've discussed ${topics.size} topics like ${topics.take(3).mkString(", ")}, and you seem $mood so far!"
  }

  private def handleSummary(): String = summarizeConversation(history)

  private def handleAndPhrase(input: String): String = {
    val recipes = ConversationBrain.recipesForAndPhrase(input).get
    lastRecommendations = recipes
    currentFlow = None
    if (recipes.nonEmpty) ResponseFormatter.formatList("Matched Recipes", recipes) + "\n\nWould you like:\n• More ideas\n• Full recipe\n• Start cooking"
    else "Hmm 👨\u200d🍳\n\nCouldn't find recipes matching both. Try adjusting your search!"
  }

  private def detectTimeSearch(input: String): Option[Int] = {
    if (input.contains("under") || input.contains("less than") || input.contains("within") || input.contains("max") || input.contains("minute"))
      ConversationBrain.detectPrepTime(input)
    else None
  }

  private def handleTimeSearch(input: String, time: Int): String = {
    val recipes = RecipeKnowledgeBase.recipes.filter(_.prepTime <= time).take(5)
    lastRecommendations = recipes
    currentFlow = None
    if (recipes.nonEmpty) ResponseFormatter.formatList(s"Ready in ≤$time Minutes", recipes) + "\n\nWould you like:\n• Full recipe\n• Start cooking"
    else s"Hmm 👨\u200d🍳\n\nNo recipes found under $time minutes. Try a higher time!"
  }

  private def handleHungry(): String = {
    currentFlow = Some("meal_choice")
    "No worries 👨‍🍳\n\nLet's figure it out together.\nWhat sounds better right now?\n\n• Quick meal\n• Comfort food\n• Healthy food\n• Snack"
  }

  private def handleQuickMealFlow(input: String, user: String): String = {
    if (input.contains("quick") || input.contains("fast")) {
      currentFlow = Some("quick_meal")
      "Nice choice ⚡\n\nHow much time do you have?\n\n• 10 minutes\n• 20 minutes\n• 30+ minutes"
    } else if (input.contains("10") || input.contains("ten")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(_.prepTime <= 10).take(5)
      lastRecommendations = recipes
      ResponseFormatter.formatList("Super Quick (≤10 min)", recipes) + "\n\nWould you like:\n• More ideas\n• Full recipe\n• Start cooking"
    } else if (input.contains("20") || input.contains("twenty")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(_.prepTime <= 20).take(5)
      lastRecommendations = recipes
      ResponseFormatter.formatList("Quick Meals (≤20 min)", recipes) + "\n\nWould you like:\n• More ideas\n• Full recipe\n• Start cooking"
    } else if (input.contains("30") || input.contains("thirty") || input.contains("more time") || input.contains("hour")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(_.prepTime <= 60).take(5)
      lastRecommendations = recipes
      ResponseFormatter.formatList("Meals (≤60 min)", recipes) + "\n\nWould you like:\n• More ideas\n• Full recipe\n• Start cooking"
    } else handleDefaultSearch(input, input, user)
  }

  private def handleComfortFoodFlow(input: String, user: String): String = {
    if (input.contains("comfort") || input.contains("comfort food")) {
      currentFlow = Some("comfort_food")
      "Now we're talking 😋\n\nWhat sounds better?\n\n• Pasta\n• Cheesy food\n• Spicy food\n• Chicken"
    } else if (input.contains("pasta")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(_.cuisine.equalsIgnoreCase("Italian")).take(5)
      lastRecommendations = recipes
      "Excellent choice 🍝\n\n" + recipes.map(r => s"• ${r.name}").mkString("\n") + "\n\nWould you like:\n• More ideas\n• Full recipe\n• Start cooking"
    } else if (input.contains("cheesy") || input.contains("cheese")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(_.ingredients.exists(_.toLowerCase.contains("cheese"))).take(5)
      lastRecommendations = recipes
      "Cheesy and delicious 🧀\n\n" + ResponseFormatter.formatList("Cheesy Recipes", recipes) + "\n\nWould you like:\n• Full recipe\n• Start cooking"
    } else if (input.contains("spicy") || input.contains("spice")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(_.dietaryTags.exists(_.equalsIgnoreCase("Spicy"))).take(5)
      lastRecommendations = recipes
      "Bring the heat 🌶️\n\n" + ResponseFormatter.formatList("Spicy Recipes", recipes) + "\n\nWould you like:\n• Full recipe\n• Start cooking"
    } else if (input.contains("chicken")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(_.dietaryTags.exists(_.equalsIgnoreCase("Chicken"))).take(5)
      lastRecommendations = recipes
      "Classic comfort chicken 🍗\n\n" + ResponseFormatter.formatList("Chicken Recipes", recipes) + "\n\nWould you like:\n• Full recipe\n• Start cooking"
    } else handleDefaultSearch(input, input, user)
  }

  private def handleHealthyFoodFlow(input: String, user: String): String = {
    if (input.contains("healthy")) {
      currentFlow = Some("healthy_food")
      "Got it 🥗\n\nWhat's your goal?\n\n• High protein\n• Low calories\n• Balanced meals\n• Weight loss"
    } else if (input.contains("protein") || input.contains("protine") || input.contains("proteen")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(_.dietaryTags.exists(_.equalsIgnoreCase("High Protein"))).take(5)
      lastRecommendations = recipes
      "Great choice 💪\n\n" + recipes.map(r => s"• ${r.name}").mkString("\n") + "\n\nWould you like:\n• More ideas\n• Full recipe\n• Start cooking"
    } else if (input.contains("low calorie") || input.contains("calorie") || input.contains("low cal")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(r => r.dietaryTags.exists(t => t.equalsIgnoreCase("Vegan") || t.equalsIgnoreCase("Low Carb"))).take(5)
      lastRecommendations = recipes
      "Light and nutritious 🥦\n\n" + ResponseFormatter.formatList("Low Calorie Recipes", recipes) + "\n\nWould you like:\n• Full recipe\n• Start cooking"
    } else if (input.contains("balanced") || input.contains("weight loss") || input.contains("diet") || input.contains("weight")) {
      currentFlow = None
      val recipes = RecipeKnowledgeBase.recipes.filter(r => r.dietaryTags.exists(t => t.equalsIgnoreCase("Vegetarian") || t.equalsIgnoreCase("Vegan")) && r.prepTime <= 45).take(5)
      lastRecommendations = recipes
      "Smart choices for your goals 🎯\n\n" + ResponseFormatter.formatList("Balanced Meals", recipes) + "\n\nWould you like:\n• Full recipe\n• Start cooking"
    } else handleDefaultSearch(input, input, user)
  }

  private def handleMultipleIngredients(mentionedIngredients: List[String]): String = {
    val recipes = RecipeKnowledgeBase.recipes.filter { recipe =>
      val matchedCount = mentionedIngredients.count(ing => recipe.ingredients.exists(_.toLowerCase.contains(ing)))
      matchedCount >= 2
    }.take(5)
    lastRecommendations = recipes
    if (recipes.nonEmpty) "Great combo 👨\u200d🍳\n\nYou could make:\n\n" + recipes.map(r => s"• ${r.name}").mkString("\n") + "\n\nWould you like:\n• Something quick\n• High protein\n• Comfort food"
    else "I couldn't find exact matches 👨‍🍳\n\nBut you could still try:\n• Fried rice\n• Pasta bowls\n• Sandwich wraps"
  }

  private def handleRecommend(user: String): String = {
    val recommended = RecommendationEngine.recommend(user = user, contextCuisine = lastCuisineContext)
    lastRecommendations = recommended
    if (recommended.nonEmpty) {
      val r = recommended.head
      val explanation = RecommendationEngine.explainRecommendation(user, r)
      s"$explanation\n\nHere are some ideas 👨\u200d🍳:\n" + recommended.map(r => s"• ${r.name} (${r.cuisine})").mkString("\n") + "\n\nWould you like:\n• More ideas\n• Full recipe\n• Start cooking"
    }
    else "I need to learn your taste first 👨‍🍳\n\nTry talking about cuisines or ingredients you enjoy!"
  }

  private def handleMore(): String = {
    val currentNames = lastRecommendations.map(_.name)
    val moreRecipes  = RecipeKnowledgeBase.recipes.filterNot(r => currentNames.contains(r.name)).take(5)
    lastRecommendations = moreRecipes
    "Here are more ideas for you 🍽️\n\n" + moreRecipes.map(r => s"• ${r.name} (${r.cuisine})").mkString("\n")
  }

  private def handleStartCooking(input: String): String = {
    val recipeOpt = RecipeKnowledgeBase.recipes.find(r => input.contains(r.name.toLowerCase))
    recipeOpt.orElse(lastRecommendations.headOption) match {
      case Some(recipe) =>
        cookingRecipe = Some(recipe)
        cookingStep   = 0
        s"Let's cook ${recipe.name} 👨\u200d🍳\n\nStep 1:\n${recipe.instructions.head}\n\nType 'done' when finished."
      case None =>
        "Tell me which recipe you'd like to cook 👨\u200d🍳\n\nOr ask me to 'recommend' something first!"
    }
  }

  private def handleCookingStep(): String = {
    val recipe = cookingRecipe.get
    cookingStep += 1
    if (cookingStep < recipe.instructions.length) s"Great 👨\u200d🍳\n\nStep ${cookingStep + 1}:\n${recipe.instructions(cookingStep)}\n\nType 'done' when finished."
    else {
      cookingRecipe = None
      cookingStep   = 0
      s"Amazing job 👨\u200d🍳🔥\n\nYour ${recipe.name} is ready! Enjoy your meal 😋"
    }
  }

  private def handleKnownRecipe(input: String): String = {
    val recipe = RecipeKnowledgeBase.recipes.find(r => input.contains(r.name.toLowerCase)).get
    lastRecommendations = List(recipe)
    ResponseFormatter.formatRecipe(recipe) + "\n\nWould you like:\n• Start cooking\n• More recipes\n• Similar dishes"
  }

  private def handleDefaultSearch(input: String, userInput: String, user: String): String = {
    currentFlow = None
    
    // Only search or generate if the input is food-related
    if (ConversationBrain.isFoodRelated(input)) {
      val prefs = PreferenceManager.loadPrefs(user)
      val avoidList = prefs.get("avoid").map(_.split(",").map(_.trim.toLowerCase).toList).getOrElse(Nil)
      
      val matchedRecipes = ConversationBrain.smartRankedSearch(input)
        .filterNot(r => avoidList.exists(a => r.cuisine.equalsIgnoreCase(a) || r.dietaryTags.exists(_.equalsIgnoreCase(a))))

      if (matchedRecipes.nonEmpty) {
        lastRecommendations = matchedRecipes
        return "I found these recipes for you 👨\u200d🍳\n\n" + matchedRecipes.map(r => s"• ${r.name} (${r.cuisine})").mkString("\n") + "\n\nWould you like:\n• More ideas\n• Full recipe\n• Start cooking"
      }
      
      // If we are here, it means we found nothing OR everything was filtered by avoidList
      val allMatchesRaw = ConversationBrain.smartRankedSearch(input)
      if (allMatchesRaw.nonEmpty && matchedRecipes.isEmpty) {
        val avoided = allMatchesRaw.head
        return s"I found '${avoided.name}', but it contains something you're avoiding (like ${avoided.dietaryTags.mkString(", ")}). \n\nWould you like me to suggest something else instead?"
      }

      // If food-related but not in DB at all
      "I couldn't find exactly that in my recipe book 👨‍🍳\n\nTry searching for something else, or ask me to 'recommend' a dish!"
    } 
    else {
      // General chatter fallback
      "I'm not sure I follow 👨‍🍳\n\nAre you looking for a specific recipe, or should I suggest something for you? Try saying 'recommend something'!"
    }
  }
}