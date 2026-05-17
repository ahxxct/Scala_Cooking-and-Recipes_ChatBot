package chatBot.recommend

import chatBot.data._
import chatBot.engine._
import scala.util.Random

/**
 * RecommendationEngine: The Decision-Making Unit.
 * 
 * This module implements a Weighted Scoring Algorithm to rank recipes.
 * It considers user preferences (Cuisines, Tags) and explicitly penalizes
 * items in the 'Avoid List' to ensure a personalized experience.
 */
object RecommendationEngine {

    /**
     * recommend: Generates a list of recipes ranked by user preferences.
     * 1. Merges persistent preferences (from file) with current context.
     * 2. Filters out avoided items based on a hard penalty score.
     * 3. Ranks remaining recipes by relevance.
     */
    def recommend(user: String, contextCuisine: Option[String] = None): List[Recipe] = {
        val prefs = PreferenceManager.loadPrefs(user)
        val cuisinePref = (prefs.get("cuisine").toList ++ contextCuisine.toList).distinct
        val tagPrefs = prefs.get("tag").getOrElse("").split(",").map(_.trim).filter(_.nonEmpty).toList
        val avoidList = prefs.get("avoid").map(_.split(",").map(_.trim.toLowerCase).toList).getOrElse(Nil)
        
        // Calculate scores for all recipes and rank them
        val scored = RecipeKnowledgeBase.recipes.map { r => (r, calculateScore(r, cuisinePref, tagPrefs, avoidList)) }
        val ranked = scored.filter(_._2 > -50).sortBy(-_._2).map(_._1) // Keep only non-penalized recipes
        
        // Return a shuffled subset of the top results for variety
        if (ranked.nonEmpty) Random.shuffle(ranked.take(10)).take(5) 
        else Random.shuffle(RecipeKnowledgeBase.recipes).take(5)
    }

    /**
     * calculateScore: The mathematical core of the recommendation.
     * - Cuisines: +20 points (High priority)
     * - Tags: +10 points (Medium priority)
     * - Avoid List: -100 points (Hard penalty)
     */
    def calculateScore(recipe: Recipe, cuisines: List[String], tags: List[String], avoid: List[String]): Int = {
        var score = 0
        
        // Penalty logic for avoided items
        if (avoid.exists(a => recipe.cuisine.equalsIgnoreCase(a) || recipe.dietaryTags.exists(_.equalsIgnoreCase(a)))) {
            score -= 100
        }
        
        // Boost for preferred cuisines
        if (cuisines.exists(_.equalsIgnoreCase(recipe.cuisine))) score += 20
        
        // Boost for matching dietary tags
        score += recipe.dietaryTags.count(t => tags.exists(_.equalsIgnoreCase(t))) * 10
        
        score
    }

    /**
     * explainRecommendation: Generates a human-friendly explanation for the AI's choice.
     */
    def explainRecommendation(user: String, recipe: Recipe): String = {
        val prefs = PreferenceManager.loadPrefs(user)
        if (prefs.get("cuisine").exists(_.equalsIgnoreCase(recipe.cuisine))) 
            s"Since you love ${recipe.cuisine} food, I think you'll enjoy this!"
        else if (recipe.dietaryTags.exists(t => prefs.get("tag").exists(_.contains(t))))
            "Based on your dietary preferences, this looks like a perfect match."
        else "I picked this one because it's a community favorite!"
    }
}