# 👨‍🍳 Koki Woki ChefBot
## Comprehensive Technical Reference Report
### Objects · Built-in Functions · Architecture · Dual GUI/CLI Interfaces

---

## 1. Project Overview

Koki Woki ChefBot is an advanced conversational cooking assistant written in **Scala 3**. The system features a sophisticated **dual-interface architecture**:
1. A **Modern Desktop GUI** built with **Scala Swing** and **FlatLaf** dark mode.
2. A **Command-Line Interface (CLI)** for console-based interaction.

The system is highly modular, split across ten source files covering user authentication, recipe database schemas, persistent preferences, natural language processing (NLP) analysis, recommendation logic, session persistence, and custom graphical view layers.

### Codebase File Catalog

| Source File | Package Path | Architectural Responsibility |
| :--- | :--- | :--- |
| **`KokiWokiGUI.scala`** | `chatBot.gui` | Instantiates the desktop GUI application. Controls custom painted panels, glassmorphism UI layouts, event listeners, and thread-safe dynamic scrolling. |
| **`ChatController.scala`** | `chatBot.controller` | Mediates data flow between the Swing view and the core chatbot routing layers; manages user auth sessions and preference states for the GUI. |
| **`CookingChatBot.scala`** | *(top-level)* | CLI execution loop, login prompts, Standard I/O validation, and session resuming. |
| **`ChatBotInteract.scala`** | `chatBot.ui` | Core routing router (`Chatbot` object). Parses intents, matches regex patterns, executes multi-step state dialogs, and appends to in-memory chat histories. |
| **`ConversationBrain.scala`** | `chatBot.brain` | Natural Language processing logic: smart ranked keyword search, time-constraint detection, sentiment/mood calculation, and topic aggregation. |
| **`Recipes.scala`** | `chatBot.data` | Database domain: holds structural case classes (`Recipe`, `InteractionEntry`) and the 46-recipe dataset. |
| **`Recommendations.scala`** | `chatBot.recommend` | Custom recommendation engine: applies multi-constraint progressive relaxation and generates explainable natural language reasons. |
| **`Preferences.scala`** | `chatBot.engine` | Disk-backed user preference manager; writes/reads active favorite cuisines and avoided tags. |
| **`LogIn.scala`** | `chatBot.auth` | Security layer: handles local registration, password file validations, and session summaries. |
| **`FormattedResponse.scala`** | `chatBot.response` | Stateless layout formatting for recipes, lists, guides, and warning banners. |

---

## 2. Architecture & Package Layout

The codebase follows a strict layered architecture to maintain high decoupling. Dependencies flow upwards; lower layers have no awareness of the structures above them.

```text
Layer 1 — Presentation Layer (UI)
   └── KokiWokiGUI.scala (ModernKokiWokiUI) · CookingChatBot.scala (startChefBot)
Layer 2 — Mediation Layer (Controllers)
   └── ChatController.scala (ChatController)
Layer 3 — Core Application Engine
   └── ChatBotInteract.scala (Chatbot Router)
Layer 4 — Services / Business Logic
   └── UserAuth · PreferenceManager · RecommendationEngine · ResponseFormatter
Layer 5 — Intelligence (NLP / Analytics)
   └── ConversationBrain.scala (ConversationBrain)
Layer 6 — Domain / Data Layer
   └── Recipes.scala (RecipeKnowledgeBase)
```

---

## 3. Object Reference — Roles & Responsibilities

### 3.1 ModernKokiWokiUI (`chatBot.gui` — `KokiWokiGUI.scala`)
The presentation layer for the desktop client. It creates a sleek glassmorphic application frame.

* **Core Components**:
  * `chatPanel`: BoxPanel containing dynamic custom-painted message bubbles.
  * `scrollPane`: ScrollPane wrapping the `chatPanel` with automatic viewport adjustments.
  * `sidebar`: BorderPanel holding the logo dashboard, Summary button, Preferences viewer, and session log history.
  * `RoundedPanel`: Extends `BoxPanel` and overrides `paintComponent` to draw custom anti-aliased rounded message bubbles.
  * `GradientPanel`: Extends `BorderPanel` and overrides `paintComponent` to render background color gradients.

* **Key Methods**:
  * `startup(args)`: Initializes the application, triggers the authenticating login dialog, and loads session states.
  * `showLoginDialog()`: Launches a native UI grid dialog for login/signup authentication.
  * `addMessage(sender, message, color, right)`: Thread-safely appends a message bubble and triggers an asynchronous layout update via `SwingUtilities.invokeLater` to scroll the viewport.

---

### 3.2 ChatController (`chatBot.controller` — `ChatController.scala`)
Acts as a Mediator between the Swing view and the application engines, abstracting session tracking.

* **Key Methods**:
  * `userExists(username)` / `register(username, password)` / `login(username, password)`: Interfaces with `UserAuth` to authenticate credentials.
  * `getSummaries(username)` / `loadSession(username, sessionNum)` / `saveSession(username, sessionNum)`: Manages local database sessions.
  * `processMessage(text, username)`: Pipes inputs into `Chatbot.generateResponse` and fetches contextual recommendations.
  * `getPreferences(username)`: Retrieves active user taste preferences.

---

### 3.3 Chatbot (`chatBot.ui` — `ChatBotInteract.scala`)
The central state machine and interaction router.

* **State Variables**:
  * `history`: In-session `List[InteractionEntry]` chat logs.
  * `currentFlow`: `Option[String]` tracking active dialog flows (e.g., `quick_meal`, `comfort_food`).
  * `cookingRecipe`: `Option[Recipe]` caching the recipe currently being cooked.
  * `cookingStep`: `Int` tracking the active step index.

* **Key Methods**:
  * `generateResponse(userInput, user)`: Validates commands, updates active preference contexts, matching rule logic, and stores logs.
  * `routeInputToHandler(...)`: Branches commands (help, summary, time constraints, state guides, and semantic search).
  * `handleLoveResponse(user)`: Randomly selects loving culinary replies.

---

### 3.4 ConversationBrain (`chatBot.brain` — `ConversationBrain.scala`)
The natural language indexing and analytical core.

* **Key Methods**:
  * `detectIntent(input)`: Parses queries to classify them as Cooking, Recommendation, Greeting, or Search.
  * `getUserMood(history)`: Processes positive and negative lexical dictionaries to label mood (Positive, Neutral, Frustrated).
  * `detectPrepTime(input)`: Applies Regular Expressions `\b(\d+)\s*(min|minute|mins)\b` to extract maximum time limits.
  * `smartRankedSearch(query)`: Splices inputs into keywords and computes a weighted ranking score:
    * *Recipe Name match*: 10 pts
    * *Cuisine match*: 15 pts
    * *Ingredient match*: 5 pts
    * *Dietary Tag match*: 8 pts
  * `recipesForAndPhrase(input)`: Parses compound "and" queries, performs separate ranked searches, and calculates strict set intersections.

---

### 3.5 RecommendationEngine (`chatBot.recommend` — `Recommendations.scala`)
Calculates tailored suggestions using progressive filter relaxation.

* **Key Methods**:
  * `recommend(user, contextCuisine)`: Loads saved tastes, queries active cuisines, applies negative constraint avoidance filters, and ranks matches.
  * `explainRecommendation(user, recipe)`: Generates dynamic natural language justifications showing why a recipe fits the user profile.

---

### 3.6 PreferenceManager (`chatBot.engine` — `Preferences.scala`)
Disk-backed user preferences manager.

* **Key Methods**:
  * `loadPrefs(user)`: Parses user configuration files into an immutable `Map[String, String]`.
  * `storePref(user, key, value)`: Safely merges new preference attributes and overwrites file entries.

---

### 3.7 UserAuth (`chatBot.auth` — `LogIn.scala`)
Manages registration directories, files, and session transcripts.

* **Key Methods**:
  * `saveSession(user, num, history, topics)`: Serializes full chat histories to disk and appends descriptive session logs.
  * `loadSessionHistory(user, num)`: De-serializes plain text session logs back into structural history records.

---

## 4. Built-in & Standard-Library Functions — Complete Index

The following table catalogs the Scala 3 collections, Option combinators, Java AWT/Swing graphical modules, and `os-lib` methods utilized:

### 4.1 Collection & Monadic Option Combinators

| Function / Monad | Location | Architectural Purpose |
| :--- | :--- | :--- |
| **`.map(f)`** | All files | Transforms collections and Option contents cleanly without mutable loops. |
| **`.flatMap(f)`** | `ConversationBrain` | Maps then flattens nested lists (e.g., aggregating dietary tags). |
| **`.filter(pred)`** | `Chatbot`, `Recommendations` | Excludes recipes that do not match search criteria or target filters. |
| **`.find(pred)`** | `Chatbot`, `ConversationBrain` | Returns an `Option[T]` containing the first element matching a predicate. |
| **`.exists(pred)`** | `Chatbot`, `ConversationBrain` | Tests boolean conditions across collections (e.g., detecting keywords). |
| **`.forall(pred)`** | `Recommendations` | Vacuously true when an Option is None; acts as a branchless, safe filter. |
| **`.count(pred)`** | `ConversationBrain` | Counts occurrences (e.g., comparing positive/negative mood words). |
| **`.sortBy(f)`** | `ConversationBrain` | Sorts recipes in descending order of search relevance. |
| **`.zipWithIndex`** | `KokiWokiGUI`, `CookingChatBot` | Pairs elements with indices to render dynamic numbered cooking steps. |
| **`Random.shuffle(l)`** | `Recommendations` | Shuffles matching recipes to ensure fresh recommendations on every call. |
| **`.orElse(opt)`** | `Recommendations` | Falls back to secondary Options if the primary option is empty. |

---

### 4.2 GUI Swing, AWT & Style Operations

| Class / Method | Package Layer | UI Purpose |
| :--- | :--- | :--- |
| **`SimpleSwingApplication`** | `scala.swing` | Manages GUI life cycles, setups, and window startups. |
| **`MainFrame`** | `scala.swing` | Renders the primary desktop window frame. |
| **`BoxPanel` / `BorderPanel`** | `scala.swing` | Organizes graphical layout structures dynamically. |
| **`ScrollPane`** | `scala.swing` | Provides scrollable viewports for long conversation logs. |
| **`Graphics2D`** | `java.awt` | Implements custom graphics painting routines. |
| **`GradientPaint`** | `java.awt` | Draws smooth, aesthetic glassmorphic background colors. |
| **`RenderingHints`** | `java.awt` | Configures anti-aliasing to render perfect rounded panels. |
| **`SwingUtilities.invokeLater`** | `javax.swing` | Executes UI scroll operations safely on the Swing Event Dispatch Thread (EDT). |
| **`JOptionPane`** | `javax.swing` | Displays cross-platform dialog windows for login credentials. |
| **`FlatDarkLaf`** | `com.formdev.flatlaf` | Configures a premium flat-themed dark mode. |

---

## 5. End-to-End Data Flow Diagram

The diagram below traces how user inputs flow through both the modern GUI and CLI engines to generate formatted, context-aware responses:

```text
               [USER INPUT EVENT]
              /                  \
             /                    \
     (GUI Interface)         (CLI Interface)
   - TextField input       - StdIn.readLine()
   - Enter/Click Event     - String validation
            \                      /
             \                    /
         [ChatController]        /
         - Intermediary         /
                \              /
                 \            /
            [ChatbotRouter.generateResponse]
            - Normalizes input to lowercase
            - Intent & Sentiment Classifiers
            - Regex preparation-time detection
            - Context Extraction (cuisine, tag)
                           |
            [Exclusion / Avoidance Filtering]
            - Proactively drops avoided items
            - Generates smart warnings on match
                           |
            [Intent Routing State Machine]
            - Dynamic flow state evaluation
            - Step-by-step cooking pipelines
                           |
            [ResponseFormatter Layouts]
            - Static pretty-printing wrappers
                           |
            [UI Thread / Console Output]
            - SwingUtilities.invokeLater viewport scroll
            - StdOut terminal prints
```

---

## 6. Major Design Patterns

* **Model-View-Controller (MVC) / Mediator**:
  `ChatController` serves as a clean mediator. The presentation Swing UI (`KokiWokiGUI.scala`) does not reference core services like `UserAuth` or `PreferenceManager` directly, keeping calculations decoupled.
* **Singleton Object Pattern**:
  All major logic systems (`ConversationBrain`, `PreferenceManager`, `RecommendationEngine`, `Chatbot`) are instantiated via Scala's `object` keyword, creating efficient, JVM-safe shared instances.
* **State Machine Pattern**:
  Conversation loops and cooking guides represent dynamic states managed by `Option[String]` parameters, allowing clean, branching conversations.
* **Template Method / Graphic Paint Hooks**:
  Overrides graphical container paint hooks (`paintComponent(g: Graphics2D)`) to construct rounded corners and gradients.
