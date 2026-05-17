package chatBot.data

/**
 * Data Structures for Chat History and Recipes.
 */
case class InteractionEntry(
    id: Int,
    userInput: String,
    botResponse: String,
    intent: String
)

case class Recipe(
    name: String,
    cuisine: String,
    difficulty: String,
    ingredients: List[String],
    dietaryTags: List[String],
    prepTime: Int,
    instructions: List[String]
)

/**
 * RecipeKnowledgeBase: The static database containing all pre-defined recipes.
 */
object RecipeKnowledgeBase {
    val recipes: List[Recipe] = List(
        // 🇮🇹 ITALIAN CUISINE
        Recipe("Margherita Pizza", "Italian", "Easy", List("Dough", "Tomato Sauce", "Mozzarella", "Basil"), List("Vegetarian"), 30, List("Roll out dough", "Spread sauce", "Add cheese", "Bake at 220°C for 10 mins", "Add fresh basil")),
        Recipe("Pasta Alfredo", "Italian", "Easy", List("Pasta", "Cream", "Butter", "Parmesan"), List("Vegetarian"), 20, List("Boil pasta", "Melt butter with cream", "Stir in parmesan", "Toss pasta in sauce")),
        Recipe("Lasagna", "Italian", "Hard", List("Pasta Sheets", "Beef", "Cheese", "Tomato Sauce"), List("High Protein", "Meat"), 80, List("Cook meat sauce", "Layer sheets, meat, and cheese", "Bake at 180°C for 45 mins")),
        Recipe("Risotto", "Italian", "Medium", List("Rice", "Mushrooms", "Parmesan", "Butter"), List("Vegetarian", "Gluten Free"), 45, List("Sauté mushrooms", "Toast rice", "Add broth slowly", "Finish with butter and cheese")),
        Recipe("Bruschetta", "Italian", "Easy", List("Bread", "Tomatoes", "Garlic", "Basil"), List("Vegetarian"), 10, List("Toast bread", "Rub with garlic", "Top with tomato and basil")),
        Recipe("Tiramisu", "Italian", "Medium", List("Ladyfingers", "Coffee", "Mascarpone", "Cocoa"), List("Vegetarian", "Dessert"), 40, List("Dip ladyfingers in coffee", "Layer with mascarpone", "Dust with cocoa", "Chill")),

        // 🇪🇬 EGYPTIAN CUISINE
        Recipe("Koshari", "Egyptian", "Medium", List("Rice", "Lentils", "Pasta", "Chickpeas", "Tomato Sauce"), List("Vegan", "Vegetarian"), 45, List("Cook rice and lentils", "Boil pasta", "Make sauce", "Layer everything")),
        Recipe("Molokhia", "Egyptian", "Medium", List("Molokhia", "Garlic", "Chicken Broth"), List("Gluten Free", "Chicken"), 40, List("Boil broth", "Add molokhia", "Sauté garlic", "Combine")),
        Recipe("Falafel", "Egyptian", "Easy", List("Fava Beans", "Parsley", "Garlic", "Onions"), List("Vegan", "Vegetarian", "Gluten Free"), 25, List("Grind beans", "Form balls", "Deep fry")),
        Recipe("Mahshi", "Egyptian", "Hard", List("Zucchini", "Rice", "Herbs", "Tomato Sauce"), List("Vegetarian"), 70, List("Hollow out veggies", "Stuff with rice", "Simmer")),
        Recipe("Fatta", "Egyptian", "Hard", List("Rice", "Beef", "Toasted Bread", "Garlic Vinegar Sauce"), List("High Protein", "Meat"), 60, List("Cook beef", "Prepare garlic sauce", "Layer bread, rice, and meat", "Top with sauce")),
        Recipe("Hawawshi", "Egyptian", "Medium", List("Bread", "Minced Meat", "Onions", "Spices"), List("Meat", "High Protein"), 35, List("Mix meat with spices", "Stuff into bread", "Bake or grill until crispy")),

        // 🇮🇳 INDIAN CUISINE
        Recipe("Butter Chicken", "Indian", "Hard", List("Chicken", "Butter", "Cream", "Tomato"), List("Gluten Free", "High Protein", "Chicken"), 60, List("Marinate chicken", "Grill", "Make gravy", "Combine")),
        Recipe("Chicken Biryani", "Indian", "Hard", List("Rice", "Chicken", "Spices", "Yogurt"), List("High Protein", "Spicy", "Chicken"), 75, List("Cook spiced chicken", "Parboil rice", "Layer and steam")),
        Recipe("Chana Masala", "Indian", "Medium", List("Chickpeas", "Tomatoes", "Onions", "Spices"), List("Vegan", "Gluten Free"), 35, List("Sauté onions", "Add chickpeas", "Simmer")),
        Recipe("Palak Paneer", "Indian", "Medium", List("Spinach", "Paneer", "Cream", "Spices"), List("Vegetarian", "Gluten Free"), 30, List("Blanch spinach", "Blend to paste", "Cook with spices and paneer cubes")),

        // 🇯🇵 JAPANESE CUISINE
        Recipe("Sushi Rolls", "Japanese", "Hard", List("Rice", "Seaweed", "Salmon", "Cucumber"), List("Seafood"), 70, List("Vinegar rice", "Spread on seaweed", "Add fillings", "Roll")),
        Recipe("Ramen", "Japanese", "Hard", List("Noodles", "Egg", "Broth", "Chicken"), List("High Protein", "Chicken"), 90, List("Simmer broth", "Cook noodles", "Assemble")),
        Recipe("Spicy Tuna Bowl", "Japanese", "Medium", List("Tuna", "Rice", "Chili Sauce"), List("Seafood", "Spicy"), 30, List("Dice tuna", "Mix with sauce", "Serve over rice")),
        Recipe("Chicken Teriyaki", "Japanese", "Easy", List("Chicken", "Soy Sauce", "Ginger", "Sugar"), List("Chicken", "High Protein"), 25, List("Sear chicken", "Add sauce ingredients", "Simmer until thickened", "Serve with rice")),

        // 🇲🇽 MEXICAN CUISINE
        Recipe("Beef Tacos", "Mexican", "Easy", List("Beef", "Tortillas", "Cheese", "Lettuce"), List("High Protein", "Meat"), 25, List("Cook beef", "Warm tortillas", "Assemble")),
        Recipe("Guacamole", "Mexican", "Easy", List("Avocado", "Onions", "Tomatoes", "Lime"), List("Vegan", "Gluten Free"), 10, List("Mash avocado", "Mix veggies", "Add lime")),
        Recipe("Beef Enchiladas", "Mexican", "Hard", List("Tortillas", "Beef", "Enchilada Sauce", "Cheese"), List("Meat", "High Protein"), 50, List("Cook beef", "Fill tortillas", "Roll and place in pan", "Cover with sauce and cheese", "Bake")),
        Recipe("Chicken Quesadilla", "Mexican", "Easy", List("Tortilla", "Chicken", "Cheese", "Peppers"), List("Chicken", "Easy"), 15, List("Grill chicken", "Place on tortilla with cheese", "Fold and toast until melted")),

        // 🇨🇳 CHINESE CUISINE
        Recipe("Kung Pao Chicken", "Chinese", "Medium", List("Chicken", "Peanuts", "Chili Peppers", "Soy Sauce"), List("Chicken", "Spicy"), 30, List("Stir-fry chicken", "Add peanuts and peppers", "Pour in sauce", "Toss")),
        Recipe("Beef and Broccoli", "Chinese", "Medium", List("Beef", "Broccoli", "Soy Sauce", "Ginger"), List("Meat", "High Protein"), 25, List("Thinly slice beef", "Stir-fry with ginger", "Add broccoli and sauce", "Steam slightly")),
        Recipe("Sweet and Sour Pork", "Chinese", "Hard", List("Pork", "Pineapple", "Bell Peppers", "Vinegar", "Sugar"), List("Meat"), 45, List("Fry battered pork", "Sauté veggies", "Add sauce and pineapple", "Combine")),

        // 🇹🇭 THAI CUISINE
        Recipe("Pad Thai", "Thai", "Medium", List("Rice Noodles", "Shrimp", "Peanuts", "Bean Sprouts", "Egg"), List("Gluten Free", "Seafood"), 30, List("Soak noodles", "Stir-fry shrimp and egg", "Add noodles and sauce", "Top with peanuts")),
        Recipe("Green Curry", "Thai", "Hard", List("Chicken", "Coconut Milk", "Green Curry Paste", "Bamboo Shoots"), List("Chicken", "Spicy", "Gluten Free"), 40, List("Sauté paste", "Add coconut milk", "Add chicken and veggies", "Simmer")),

        // 🇬🇷 GREEK CUISINE
        Recipe("Greek Salad", "Greek", "Easy", List("Cucumber", "Tomato", "Feta Cheese", "Olives", "Olive Oil"), List("Vegetarian", "Gluten Free"), 15, List("Chop vegetables", "Add olives and feta", "Drizzle with oil and oregano")),
        Recipe("Souvlaki", "Greek", "Medium", List("Chicken", "Lemon", "Garlic", "Pita Bread"), List("Chicken", "High Protein"), 30, List("Marinate chicken", "Skewer and grill", "Serve in pita with tzatziki")),
        Recipe("Moussaka", "Greek", "Hard", List("Eggplant", "Beef", "Bechamel Sauce", "Potatoes"), List("Meat", "Hard"), 90, List("Fry eggplant", "Cook meat sauce", "Layer with bechamel", "Bake until golden")),

        // 🇺🇸 AMERICAN CUISINE
        Recipe("Cheeseburger", "American", "Easy", List("Beef", "Bun", "Cheese", "Lettuce", "Tomato"), List("Meat", "High Protein"), 20, List("Form patties", "Grill meat", "Melt cheese", "Assemble in toasted bun")),
        Recipe("BBQ Ribs", "American", "Hard", List("Pork Ribs", "BBQ Sauce", "Spices"), List("Meat", "High Protein"), 180, List("Rub spices on ribs", "Slow cook", "Baste with sauce", "Grill until charred")),
        Recipe("Buffalo Wings", "American", "Medium", List("Chicken Wings", "Hot Sauce", "Butter"), List("Chicken", "Spicy"), 40, List("Fry or bake wings", "Mix sauce and butter", "Toss wings until coated")),

        // 🥗 HEALTHY / VEGAN SELECTION
        Recipe("Quinoa Bowl", "Healthy", "Easy", List("Quinoa", "Avocado", "Spinach", "Chickpeas"), List("Vegan", "Gluten Free"), 20, List("Cook quinoa", "Mix ingredients")),
        Recipe("Protein Smoothie", "Healthy", "Easy", List("Banana", "Protein Powder", "Milk"), List("High Protein"), 5, List("Blend all")),
        Recipe("Tofu Stir Fry", "Vegan", "Easy", List("Tofu", "Broccoli", "Soy Sauce"), List("Vegan"), 20, List("Fry tofu", "Add broccoli", "Stir fry")),
        Recipe("Lentil Soup", "Healthy", "Easy", List("Lentils", "Carrots", "Onions", "Celery", "Broth"), List("Vegan", "Vegetarian"), 35, List("Sauté veggies", "Add lentils and broth", "Simmer until tender")),
        Recipe("Zucchini Noodles", "Healthy", "Easy", List("Zucchini", "Pesto Sauce", "Cherry Tomatoes"), List("Vegan", "Low Carb"), 15, List("Spiralize zucchini", "Sauté quickly", "Toss with pesto and tomatoes")),

        // 🥞 BREAKFAST & DESSERTS
        Recipe("Pancakes", "Breakfast", "Easy", List("Flour", "Milk", "Eggs", "Maple Syrup"), List("Vegetarian"), 20, List("Mix batter", "Pour onto griddle", "Flip when bubbly", "Serve with syrup")),
        Recipe("Oatmeal", "Breakfast", "Easy", List("Oats", "Milk", "Honey", "Fruit"), List("Vegetarian", "Healthy"), 10, List("Boil milk", "Add oats", "Simmer", "Top with honey and fruit")),
        Recipe("Chocolate Cake", "Dessert", "Medium", List("Flour", "Cocoa", "Sugar", "Eggs"), List("Vegetarian", "Dessert"), 50, List("Mix batter", "Bake at 180°C", "Cool and frost")),
        Recipe("Apple Pie", "Dessert", "Hard", List("Apples", "Flour", "Butter", "Cinnamon", "Sugar"), List("Vegetarian", "Dessert"), 90, List("Make crust", "Prepare apple filling", "Assemble pie", "Bake until golden")),
        Recipe("Fruit Salad", "Dessert", "Easy", List("Apple", "Banana", "Grapes", "Orange Juice"), List("Vegan", "Healthy"), 10, List("Chop fruit", "Toss in bowl", "Add orange juice splash"))
    )

    // Meta-Data Auto-Generation: Derived from the recipe list above
    val allCuisines:     List[String] = recipes.map(_.cuisine.toLowerCase).distinct
    val allTags:         List[String] = recipes.flatMap(_.dietaryTags).map(_.toLowerCase).distinct
    val allDifficulties: List[String] = recipes.map(_.difficulty.toLowerCase).distinct
    val allIngredients:  List[String] = recipes.flatMap(_.ingredients).map(_.toLowerCase).distinct
}