import os
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas
from reportlab.lib.units import inch

# Define cohesive corporate color palette
PRIMARY = colors.HexColor("#0A0C18")       # Deep Midnight Blue
SECONDARY = colors.HexColor("#5865F2")     # Vibrant Royal Indigo
ACCENT = colors.HexColor("#F97316")        # Bright Warm Orange
TEXT_DARK = colors.HexColor("#1E293B")     # Dark Charcoal Text
TEXT_LIGHT = colors.HexColor("#F8FAFC")    # Off-White Text
BG_LIGHT = colors.HexColor("#F1F5F9")      # Cool Light Grey
BG_HEADER = colors.HexColor("#1E293B")     # Cool Dark Header Grey

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        # Draw background elements or headers/footers
        if self._pageNumber == 1:
            # RENDER GORGEOUS COVER PAGE CANVAS DIRECTLY
            self.saveState()
            # Draw beautiful split background (Midnight Blue full page)
            self.setFillColor(PRIMARY)
            self.rect(0, 0, 8.5*inch, 11*inch, fill=True, stroke=False)
            
            # Draw orange accent block on the left
            self.setFillColor(ACCENT)
            self.rect(0, 0, 0.4*inch, 11*inch, fill=True, stroke=False)
            
            # Draw secondary accent stripe
            self.setFillColor(SECONDARY)
            self.rect(0.4*inch, 0, 0.05*inch, 11*inch, fill=True, stroke=False)
            
            # Draw decorative circular rings in bottom right corner
            self.setStrokeColor(colors.HexColor("#202440"))
            self.setLineWidth(2)
            self.circle(8.5*inch, 0, 2*inch, stroke=True, fill=False)
            self.circle(8.5*inch, 0, 2.5*inch, stroke=True, fill=False)
            self.circle(8.5*inch, 0, 3*inch, stroke=True, fill=False)

            self.restoreState()
        else:
            # RENDER STANDARD PAGE HEADERS & FOOTERS
            self.saveState()
            # Header line and text
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.5)
            self.line(0.75*inch, 10.25*inch, 7.75*inch, 10.25*inch)
            
            self.setFont("Helvetica-Bold", 8)
            self.setFillColor(SECONDARY)
            self.drawString(0.75*inch, 10.35*inch, "KOKI WOKI CHEFBOT")
            
            self.setFont("Helvetica", 8)
            self.setFillColor(colors.HexColor("#64748B"))
            self.drawRightString(7.75*inch, 10.35*inch, "Technical Reference Report")

            # Footer line and page numbers
            self.line(0.75*inch, 0.75*inch, 7.75*inch, 0.75*inch)
            self.drawString(0.75*inch, 0.55*inch, "Confidential · Academic Reference Report")
            page_text = f"Page {self._pageNumber} of {page_count}"
            self.drawRightString(7.75*inch, 0.55*inch, page_text)
            self.restoreState()


def build_pdf():
    pdf_filename = "KokiWoki_Chatbot_Report.pdf"
    
    # 0.75-inch margins (54 points)
    doc = SimpleDocTemplate(
        pdf_filename,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=72,
        bottomMargin=72
    )

    styles = getSampleStyleSheet()
    
    # Modify default styles or add custom ones
    styles.add(ParagraphStyle(
        name='CoverTitle',
        fontName='Helvetica-Bold',
        fontSize=36,
        leading=42,
        textColor=TEXT_LIGHT,
        alignment=0, # Left-aligned
        spaceAfter=15
    ))
    
    styles.add(ParagraphStyle(
        name='CoverSubtitle',
        fontName='Helvetica',
        fontSize=16,
        leading=22,
        textColor=colors.HexColor("#CBD5E1"),
        alignment=0,
        spaceAfter=30
    ))
    
    styles.add(ParagraphStyle(
        name='CoverMeta',
        fontName='Helvetica-Bold',
        fontSize=10,
        leading=16,
        textColor=ACCENT,
        alignment=0
    ))
    
    styles.add(ParagraphStyle(
        name='CoverMetaVal',
        fontName='Helvetica',
        fontSize=11,
        leading=16,
        textColor=TEXT_LIGHT,
        alignment=0,
        spaceAfter=15
    ))

    styles.add(ParagraphStyle(
        name='SectionHeading',
        fontName='Helvetica-Bold',
        fontSize=18,
        leading=22,
        textColor=PRIMARY,
        spaceBefore=15,
        spaceAfter=10,
        keepWithNext=True
    ))

    styles.add(ParagraphStyle(
        name='SubSectionHeading',
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=SECONDARY,
        spaceBefore=10,
        spaceAfter=6,
        keepWithNext=True
    ))

    styles.add(ParagraphStyle(
        name='ReportBody',
        fontName='Helvetica',
        fontSize=9.5,
        leading=14,
        textColor=TEXT_DARK,
        spaceAfter=8
    ))

    styles.add(ParagraphStyle(
        name='ReportBodyBold',
        fontName='Helvetica-Bold',
        fontSize=9.5,
        leading=14,
        textColor=TEXT_DARK,
        spaceAfter=8
    ))

    styles.add(ParagraphStyle(
        name='BulletText',
        fontName='Helvetica',
        fontSize=9.5,
        leading=13,
        textColor=TEXT_DARK,
        leftIndent=15,
        firstLineIndent=-10,
        spaceAfter=5
    ))

    styles.add(ParagraphStyle(
        name='CodeStyle',
        fontName='Courier',
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor("#0F172A"),
        backColor=colors.HexColor("#F8FAFC"),
        borderColor=colors.HexColor("#E2E8F0"),
        borderWidth=0.5,
        borderPadding=6,
        spaceAfter=10
    ))

    styles.add(ParagraphStyle(
        name='TableHeader',
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=12,
        textColor=TEXT_LIGHT,
        alignment=0
    ))

    styles.add(ParagraphStyle(
        name='TableCell',
        fontName='Helvetica',
        fontSize=8.5,
        leading=11,
        textColor=TEXT_DARK,
        alignment=0
    ))

    styles.add(ParagraphStyle(
        name='TableCellBold',
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=11,
        textColor=TEXT_DARK,
        alignment=0
    ))

    story = []

    # ================= PAGE 1: COVER PAGE =================
    story.append(Spacer(1, 1.8*inch))
    story.append(Paragraph("Koki Woki ChefBot", styles['CoverTitle']))
    story.append(Paragraph("Technical Reference & Codebase Analysis Report", styles['CoverSubtitle']))
    
    # Elegant decorative accent line
    d_table = Table([[""]], colWidths=[3*inch], rowHeights=[4])
    d_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), ACCENT),
        ('PADDING', (0,0), (-1,-1), 0),
        ('BOTTOMPADDING', (0,0), (-1,-1), 0),
        ('TOPPADDING', (0,0), (-1,-1), 0),
    ]))
    story.append(d_table)
    story.append(Spacer(1, 0.4*inch))
    
    # Description
    story.append(Paragraph("A highly scalable, multi-interface conversational cooking assistant and recipe recommendation system built with modern Software Design patterns in Scala 3.", ParagraphStyle('CoverDesc', parent=styles['ReportBody'], textColor=colors.HexColor("#E2E8F0"), fontSize=11, leading=16, spaceAfter=40)))
    
    # Metadata block
    meta_data = [
        [Paragraph("DEVELOPED BY", styles['CoverMeta']), Paragraph("ARCHITECTURAL RUNTIMES", styles['CoverMeta'])],
        [Paragraph("Hossam & Antigravity Pair", styles['CoverMetaVal']), Paragraph("Scala 3.3.1 (JVM Runtime)", styles['CoverMetaVal'])],
        [Paragraph("DATE OF SUBMISSION", styles['CoverMeta']), Paragraph("PRESENTATION LAYERS", styles['CoverMeta'])],
        [Paragraph("May 17, 2026", styles['CoverMetaVal']), Paragraph("Desktop GUI (Scala Swing + FlatLaf)<br/>Console Command-Line (CLI)", styles['CoverMetaVal'])]
    ]
    meta_table = Table(meta_data, colWidths=[2.5*inch, 3.5*inch])
    meta_table.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('PADDING', (0,0), (-1,-1), 0),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(meta_table)
    
    story.append(PageBreak())

    # ================= PAGE 2: TABLE OF CONTENTS & INTRODUCTION =================
    story.append(Paragraph("1. Project Overview & Scope", styles['SectionHeading']))
    story.append(Paragraph("Koki Woki ChefBot is an advanced conversational AI cooking assistant designed to deliver intelligent recipe discovery, user preference modeling, and proactive allergy/dietary tracking. The system features a highly-decoupled architecture composed of a desktop Graphic User Interface (GUI) and a standard Console CLI, demonstrating best practices in modular software engineering.", styles['ReportBody']))
    
    story.append(Spacer(1, 15))
    story.append(Paragraph("2. Technical Project Capabilities", styles['SectionHeading']))
    
    capabilities = [
        ("Explainable AI (XAI) Recommendation", "Utilizes user preference weights to score recipe entries, combined with an explainability module that outputs plain text rationales (e.g., 'Since you prefer Italian, you might enjoy...')"),
        ("Dual-Interface Presentation", "Supports a premium event-driven Desktop GUI utilizing custom graphic rendering, FlatLaf Dark styling, and thread-safe viewport scrolling, alongside a fallback interactive Console shell."),
        ("Multi-State Conversational State Machine", "Tracks user conversation steps and guides them step-by-step through interactive recipes, caching step numbers and dynamically validating command arguments."),
        ("Boolean Multi-Term Search & Regex Parsing", "Decodes compound queries (e.g., 'chicken and pasta') and performs strict set intersection indexing, combined with Regex parsers to capture preparation times."),
        ("Positive/Negative Preference Constraints", "Detects negative phrases (e.g., 'dont like', 'avoid') to populate disfavored lists, proactively filtering these out and serving custom warn-dialogs if a match occurs.")
    ]

    for title, desc in capabilities:
        bullet_html = f"<b>■ {title}:</b> {desc}"
        story.append(Paragraph(bullet_html, styles['BulletText']))

    story.append(PageBreak())

    # ================= PAGE 3: SOURCE FILES REFERENCE =================
    story.append(Paragraph("3. Source Modularity & File Mapping", styles['SectionHeading']))
    story.append(Paragraph("The codebase consists of 10 clean Scala classes, organized hierarchically to maintain structural decoupling. Each module operates in a dedicated package namespace.", styles['ReportBody']))
    story.append(Spacer(1, 10))

    headers = [Paragraph("Filename", styles['TableHeader']), Paragraph("Package Layer", styles['TableHeader']), Paragraph("Primary System Responsibility", styles['TableHeader'])]
    
    file_rows = [
        ("KokiWokiGUI.scala", "chatBot.gui", "Desktop GUI. Handles layouts, FlatLaf styles, rounded message bubble canvas painting, and thread-safe scrolling."),
        ("ChatController.scala", "chatBot.controller", "Mediates GUI clicks and text inputs with database layers and tracks current logged-in sessions."),
        ("CookingChatBot.scala", "(top-level)", "Entry point for the CLI. Drives command loop, terminal auth prompts, and session history restores."),
        ("ChatBotInteract.scala", "chatBot.ui", "Central chatbot router ('Chatbot'). Controls dynamic dialog state flows and routes inputs to specialized handlers."),
        ("ConversationBrain.scala", "chatBot.brain", "Natural Language Processing (NLP) layer. Smart ranked keyword search, regex parsing, and mood/topic calculations."),
        ("Recipes.scala", "chatBot.data", "Houses structural database schemas ('Recipe' and 'InteractionEntry') and the 46-recipe dataset."),
        ("Recommendations.scala", "chatBot.recommend", "Applies dynamic progressive relaxation algorithms to recommend recipes and generates explainability text."),
        ("Preferences.scala", "chatBot.engine", "Disk-backed configuration database tracking preferred and avoided ingredients/cuisines per user."),
        ("LogIn.scala", "chatBot.auth", "Authenticates login credentials, manages accounts, and serializes historical chat transcripts."),
        ("FormattedResponse.scala", "chatBot.response", "Stateless, pure string layouts formatting cards, summaries, and guides.")
    ]

    table_data = [headers]
    for filename, pkg, desc in file_rows:
        table_data.append([
            Paragraph(filename, styles['TableCellBold']),
            Paragraph(pkg, styles['TableCell']),
            Paragraph(desc, styles['TableCell'])
        ])

    file_table = Table(table_data, colWidths=[1.6*inch, 1.4*inch, 4.0*inch])
    file_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), PRIMARY),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, BG_LIGHT]),
        ('PADDING', (0,0), (-1,-1), 6),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
    ]))
    story.append(file_table)
    
    story.append(PageBreak())

    # ================= PAGE 4: DETAILED ARCHITECTURE AND MVC MEDIATOR =================
    story.append(Paragraph("4. Architectural Modularity & Mediator Pattern", styles['SectionHeading']))
    story.append(Paragraph("To support both Graphical and Console interactions without duplicating code, Koki Woki ChefBot implements a clean **Model-View-Controller (MVC) / Mediator** architectural pattern. The system is layered vertically with no circular dependencies.", styles['ReportBody']))
    
    story.append(Spacer(1, 10))
    
    story.append(Paragraph("Architectural Layout Hierarchy", styles['SubSectionHeading']))
    
    layers = [
        ("Layer 1: Presentation Layer", "Handles all input/output renderings. Includes ModernKokiWokiUI (Scala Swing Desktop View) and startChefBot (Terminal Interactive Console View)."),
        ("Layer 2: Mediation Layer", "Represented by ChatController.scala. It abstracts all session tracking, authentication handshakes, and preference state updates, shielding the Swing view from directly referencing core logical engines."),
        ("Layer 3: Core Application Router", "Represented by ChatBotInteract.scala. Orchestrates the flow of queries, matches intent states, and updates dynamic active step-by-step guides."),
        ("Layer 4: Business Services Layer", "Consists of specialized, stateless utility components: UserAuth (identity operations), PreferenceManager (likes/dislikes persistence), RecommendationEngine (taste recommendations), and ResponseFormatter (pretty prints)."),
        ("Layer 5: Analytical Intelligence Layer", "Consists of ConversationBrain.scala. Houses NLP calculation utilities: weighted search indices, regex extractors, and emotional classifiers.")
    ]
    
    for title, desc in layers:
        bullet_html = f"<b>■ {title}:</b> {desc}"
        story.append(Paragraph(bullet_html, styles['BulletText']))
        
    story.append(Spacer(1, 15))
    story.append(Paragraph("Advantages of the Mediator Architecture", styles['SubSectionHeading']))
    story.append(Paragraph("1. <b>Zero Presentation Coupling:</b> The GUI code is purely graphical and contains zero chatbot brain logic, which is completely mediated by the Controller.<br/>"
                           "2. <b>Reusable Logical Core:</b> The exact same `Chatbot` router and brain files are shared between the CLI and GUI, ensuring 100% consistent feature matching.<br/>"
                           "3. <b>Multi-session Consistency:</b> The Controller reads preferences directly from the file database on every call, avoiding stale state caches.", styles['ReportBody']))

    story.append(PageBreak())

    # ================= PAGE 5: DYNAMIC CONVERSATIONAL STATE MACHINE =================
    story.append(Paragraph("5. Dynamic Conversational State Machine", styles['SectionHeading']))
    story.append(Paragraph("Traditional chatbots rely on flat, single-turn search. Koki Woki ChefBot employs an encapsulated, conversational State Machine to drive interactive step-by-step processes and conversational branches.", styles['ReportBody']))
    
    story.append(Spacer(1, 10))
    story.append(Paragraph("1. Step-by-Step Interactive Cooking Guide", styles['SubSectionHeading']))
    story.append(Paragraph("When a user asks to cook a recipe (e.g., 'start cooking lasagna'), the state machine triggers a cooking pipeline:<br/>"
                           "• The current recipe schema is cached in the `cookingRecipe: Option[Recipe]` state parameter.<br/>"
                           "• The step pointer `cookingStep` is initialized to 0.<br/>"
                           "• The chatbot displays the first instruction, transitioning to the active cooking state.<br/>"
                           "• The main router intercepts subsequent inputs. When the user types 'done', the pointer increments and the next step is dynamically formatted and returned. Typing done on the final step resets all state pointers and celebrates completion.", styles['ReportBody']))
    
    story.append(Spacer(1, 10))
    story.append(Paragraph("2. Context-Aware Sub-Dialog Flows", styles['SubSectionHeading']))
    story.append(Paragraph("To solve the 'cold-start' recipe discovery issue, the state machine tracks branching paths (e.g., `quick_meal`, `comfort_food`, `healthy_food`) via an encapsulated `currentFlow: Option[String]` variable:<br/>"
                           "• **State Transition:** Typing 'quick meal' transitions `currentFlow` to `Some(\"quick_meal\")` and asks 'How much time do you have? 10, 20, or 30+ minutes?'.<br/>"
                           "• **Branch Execution:** The subsequent user entry (e.g., '10') is routed specifically under the active flow context, fetching matching recipes immediately and then resetting the flow state to `None`.", styles['ReportBody']))

    story.append(PageBreak())

    # ================= PAGE 6: SEARCH & CONSTRAINT MODELLING =================
    story.append(Paragraph("6. Semantic Search & Negative Preferences", styles['SectionHeading']))
    story.append(Paragraph("The system features a weighted relevance indexing engine combined with smart exclusion filtering, protecting users from allergies and food dislikes.", styles['ReportBody']))
    
    story.append(Spacer(1, 10))
    story.append(Paragraph("1. Weighted Multi-Attribute Relevance Index", styles['SubSectionHeading']))
    story.append(Paragraph("When search queries are submitted, keywords are tokenized and scored across multiple data layers:<br/>"
                           "• **Recipe Name Match:** +10 points<br/>"
                           "• **Cuisine Match:** +15 points<br/>"
                           "• **Ingredient Match:** +5 points<br/>"
                           "• **Dietary Tag Match:** +8 points<br/>"
                           "Results are sorted descending, returning only highly relevant, targeted suggestions.", styles['ReportBody']))

    story.append(Spacer(1, 10))
    story.append(Paragraph("2. Proactive Negative Constraint Filtering", styles['SubSectionHeading']))
    story.append(Paragraph("Users can specify items they wish to avoid (e.g., 'I hate spicy food' or 'no chicken'). The PreferenceManager extracts these parameters and saves them under the `avoid` key. The engines then execute exclusion filtering:<br/>"
                           "• **Query Filtering:** During searches and recommendation computations, recipes containing disfavored cuisines or tags are filtered out completely.<br/>"
                           "• **Intelligent Warning:** If a high-scoring recipe matches the query term but contains an avoided element, it is omitted, and the chatbot displays a customized dialog: <i>'I found Recipe X, but it contains something you are avoiding (Spicy). Would you like me to recommend something else?'</i>", styles['ReportBody']))

    story.append(PageBreak())

    # ================= PAGE 7: BUILT-IN COLLECTION INDEX (PART 1) =================
    story.append(Paragraph("7. Scala 3 Collection API & Monadic Index", styles['SectionHeading']))
    story.append(Paragraph("The codebase stands as a model for pure Functional Programming, leveraging monadic collections and higher-order functions to execute branchless, safe calculations.", styles['ReportBody']))
    story.append(Spacer(1, 10))

    headers2 = [Paragraph("Function", styles['TableHeader']), Paragraph("Applied Module", styles['TableHeader']), Paragraph("Functional Software Purpose", styles['TableHeader'])]
    
    c_rows = [
        (".map(f)", "All modules", "Transforms elements of Lists or monadic Options cleanly without side-effects."),
        (".filter(pred)", "Chatbot / Search", "Keeps only elements satisfying a predicate; excludes disfavored items."),
        (".flatMap(f)", "ConversationBrain", "Maps then flattens nested lists (e.g., aggregating distinct ingredients across recipes)."),
        (".find(pred)", "Chatbot Router", "Returns Option[T] containing the first matching element, preventing null failures."),
        (".exists(pred)", "ConversationBrain", "Tests boolean criteria across a list (e.g., looking for disallowed items)."),
        (".forall(pred)", "RecommendationEngine", "Vacuously true when an Option is None; acts as a branchless safe filter."),
        (".count(pred)", "ConversationBrain", "Counts matching elements (e.g., tracking emotional keywords in chat histories)."),
        (".distinct", "ConversationBrain", "Deduplicates autocomplete suggestion lists and aggregated topics."),
        (".sortBy(f)", "ConversationBrain", "Sorts search results in descending order based on weighted score integers."),
        (".zipWithIndex", "KokiWokiGUI", "Pairs items with indexes to render numbered instructions dynamically."),
        ("Random.shuffle", "Recommendations", "Randomly shuffles recommendations to provide fresh suggestions.")
    ]

    table_data2 = [headers2]
    for func, mod, desc in c_rows:
        table_data2.append([
            Paragraph(func, styles['TableCellBold']),
            Paragraph(mod, styles['TableCell']),
            Paragraph(desc, styles['TableCell'])
        ])

    col_table = Table(table_data2, colWidths=[1.6*inch, 1.4*inch, 4.0*inch])
    col_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), PRIMARY),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, BG_LIGHT]),
        ('PADDING', (0,0), (-1,-1), 5),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
    ]))
    story.append(col_table)

    story.append(PageBreak())

    # ================= PAGE 8: BUILT-IN SWING & AWT INDEX (PART 2) =================
    story.append(Paragraph("8. Java AWT & Scala Swing API Index", styles['SectionHeading']))
    story.append(Paragraph("The presentation view layer harnesses the full capability of the Swing layout framework combined with custom anti-aliased graphics painting to produce a responsive desktop client.", styles['ReportBody']))
    story.append(Spacer(1, 10))

    headers3 = [Paragraph("Graphical API", styles['TableHeader']), Paragraph("Applied Module", styles['TableHeader']), Paragraph("UI & Layout Responsibility", styles['TableHeader'])]
    
    gui_rows = [
        ("SimpleSwingApplication", "KokiWokiGUI.scala", "Manages the Swing application life cycle, window startups, and frame rendering."),
        ("MainFrame", "KokiWokiGUI.scala", "Renders the primary desktop application frame and window metadata."),
        ("BoxPanel / BorderPanel", "KokiWokiGUI.scala", "Organizes sub-panels vertically, horizontally, or across cardinal boundaries."),
        ("ScrollPane", "KokiWokiGUI.scala", "Wraps chat containers to provide fluid, automatic scrolling viewports."),
        ("Graphics2D", "KokiWokiGUI.scala", "Provides advanced anti-aliased custom painting routines."),
        ("GradientPaint", "KokiWokiGUI.scala", "Generates smooth, professional background color gradients for glassmorphism panels."),
        ("RenderingHints", "KokiWokiGUI.scala", "Configures graphics rendering pipeline for beautiful, rounded edge bubbles."),
        ("SwingUtilities.invokeLater", "KokiWokiGUI.scala", "Safely schedules scroll updates on the Swing Event Dispatch Thread (EDT)."),
        ("JOptionPane", "KokiWokiGUI.scala", "Displays native dialog popups for registration prompts and password inputs."),
        ("FlatDarkLaf", "KokiWokiGUI.scala", "Sets up the flatdark-skinned Look-and-Feel theme interface.")
    ]

    table_data3 = [headers3]
    for api, mod, desc in gui_rows:
        table_data3.append([
            Paragraph(api, styles['TableCellBold']),
            Paragraph(mod, styles['TableCell']),
            Paragraph(desc, styles['TableCell'])
        ])

    gui_table = Table(table_data3, colWidths=[2.0*inch, 1.4*inch, 3.6*inch])
    gui_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), PRIMARY),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, BG_LIGHT]),
        ('PADDING', (0,0), (-1,-1), 5),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
    ]))
    story.append(gui_table)

    story.append(PageBreak())

    # ================= PAGE 9: END-TO-END DATA FLOW =================
    story.append(Paragraph("9. End-to-End System Data Flow", styles['SectionHeading']))
    story.append(Paragraph("The system handles user inputs through a unified processing pipeline, converting raw textual and graphical events into context-aware responses and recommendations.", styles['ReportBody']))
    
    story.append(Spacer(1, 10))

    flow_steps = [
        ("Step 1: Event Dispatch", "User submits text via GUI TextField or Console StdIn. readline. GUI events trigger actionPerformed listeners, dispatching inputs to the Controller."),
        ("Step 2: Controller Mediation", "ChatController intercepts the message, checks user auth state, and dispatches the payload to the Chatbot router."),
        ("Step 3: Intent & Context Extraction", "Chatbot normalizes inputs to lowercase. It calls ConversationBrain to check mood, parse cuisines, extract tags, and detect prep-times via Regex."),
        ("Step 4: Preference Constraint Merge", "If preference keywords (e.g. 'I hate') are matched, PreferenceManager stores dislikes/likes in local configuration files on disk."),
        ("Step 5: Proactive Exclusion Filtering", "Calculates searches and recommendations, automatically omitting recipes matching disfavored ingredients or tags, serving warnings if a direct match occurs."),
        ("Step 6: Dialog State Routing", "Evaluates conversational flow variables (Option[String]). Routes queries to specialized flows or the step-by-step cooking engine."),
        ("Step 7: Stateless Formatting", "Pipes recipe lists and guide strings to the ResponseFormatter to pretty-print structured visual cards."),
        ("Step 8: Thread-Safe UI Update", "The UI receives the formatted text. In the GUI, SwingUtilities.invokeLater is called to recalculate scroll heights and scroll the chat view to the bottom.")
    ]

    for step, desc in flow_steps:
        bullet_html = f"<b>{step}:</b> {desc}"
        story.append(Paragraph(bullet_html, styles['BulletText']))
        story.append(Spacer(1, 6))

    story.append(PageBreak())

    # ================= PAGE 10: KEY DESIGN PATTERNS =================
    story.append(Paragraph("10. Structural Software Design Patterns", styles['SectionHeading']))
    story.append(Paragraph("Koki Woki ChefBot implements standard structural and behavioral software engineering patterns to ensure high maintenance, performance, and decoupling:", styles['ReportBody']))
    
    story.append(Spacer(1, 10))
    
    patterns = [
        ("Model-View-Controller (MVC) / Mediator", "Decouples View containers (KokiWokiGUI) from database services. ChatController acts as a mediator, abstracting logins, preference retrievals, and engine dispatches."),
        ("Singleton Object Pattern", "Scala's object keyword creates JVM-safe single shared instances for all services (ConversationBrain, RecommendationEngine, PreferenceManager, ResponseFormatter), saving memory."),
        ("State Machine Pattern", "Conversational loops and cooking guidelines represent dynamic states managed by Option[String] parameters, allowing branching conversations."),
        ("Template Method / Graphics Painting Hooks", "Overrides paintComponent(g: Graphics2D) to draw rounded bubble chat corners and custom background gradients, providing a beautiful custom UI."),
        ("Progressive Relaxation Pattern", "RecommendationEngine tries the most specific filter combination first (cuisine + tag + difficulty) and progressively drops criteria if no recipes are found, ensuring the user always receives recommendations.")
    ]

    for name, desc in patterns:
        story.append(Paragraph(f"<b>■ {name}:</b>", styles['SubSectionHeading']))
        story.append(Paragraph(desc, styles['ReportBody']))
        story.append(Spacer(1, 4))
        
    story.append(Spacer(1, 20))
    story.append(Paragraph("<font color='#0A0C18'><b>End of Report — Koki Woki ChefBot Technical Reference</b></font>", ParagraphStyle('EndReport', parent=styles['ReportBody'], alignment=1)))

    # Build the document
    doc.build(story, canvasmaker=NumberedCanvas)


if __name__ == "__main__":
    build_pdf()
