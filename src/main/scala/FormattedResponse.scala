package chatBot.response
//> using dep com.lihaoyi::os-lib:0.11.8
import scala.io._
import os._
import scala.util.Random
import scala.util.Random
import chatBot.data._
import chatBot.brain._
import chatBot.auth._
import chatBot.engine._
import chatBot.recommend._
import chatBot.ui._

// ===============================================
// SECTION 6: RESPONSE FORMATTER
// ===============================================

object ResponseFormatter {

    def greetingResponse(user: String): String =
        s"""Hi, Chef $user! Welcome to Koki Woki.
            |I can help you find recipes, explore cuisines, and suggest meals based on your taste.
            |Type 'help' anytime to see what I can do!""".stripMargin//lines start from the pipeline "|"

    def formatRecipe(r: Recipe): String =
        s"""|
            |=== ${r.name} ===
            | Cuisine    : ${r.cuisine}
            | Difficulty : ${r.difficulty}
            | Prep Time  : ${r.prepTime} min
            | Tags       : ${r.dietaryTags.mkString(", ")}
            |
            | Ingredients: ${r.ingredients.mkString(", ")}
            |
            | Steps:
            |${r.instructions.zipWithIndex.map { case (s, i) => s"  ${i + 1}. $s" }.mkString("\n")}
            |""".stripMargin //ordered steps, 

    def formatList(title: String, rs: List[Recipe]): String = {

        if (rs.isEmpty)
            return s"I couldn't find any $title recipes. Try another keyword!"

        val intros = List( "Nice choice 👨‍🍳", "Here are some delicious options:", "You might enjoy these recipes:",
        "I found these recipes for you:","These look tasty 🍽️")

        val intro = Random.shuffle(intros).head

        intro + "\n\n" +
        rs.map(r =>
            s"• ${r.name} (${r.cuisine} - ${r.difficulty} - ${r.prepTime} min)"
        ).mkString("\n")
    }
    
    def showGuide(): String =
        """|
           |=== CHEFBOT GUIDE ===
           |
           |  Search by cuisine  : 'Italian', 'Egyptian'
           |  Search by tag      : 'Vegan', 'Gluten Free'
           |  Search by level    : 'Easy', 'Hard'
           |  Recipe details     : 'How to make Ramen'
           |                       or just type 'Ramen'
           |  Save preference    : 'I love Italian'
           |                       'I prefer Easy meals'
           |                       'I am Vegan'
           |  Conversation tools : 'topics'
           |  Exit               : 'logout' or 'exit'
           |
           |=====================""".stripMargin

    def repeatedQueryNote(): String =
        "(You've asked about this before - here's a refresher!)\n"

    def preferenceSaved(key: String, value: String): String =
        s"Got it! I'll remember you prefer $value $key from now on."
}