//> using dep org.scala-lang.modules::scala-swing:3.0.0
//> using dep com.formdev:flatlaf:3.4

package chatBot.gui

import scala.swing.*
import scala.swing.event.*
import javax.swing.*
import java.awt.{Color, Dimension, Font, GradientPaint, Graphics2D, RenderingHints}
import com.formdev.flatlaf.FlatDarkLaf

import chatBot.engine.PreferenceManager
import chatBot.controller.*

/**
 * ModernKokiWokiUI: The visual front-end for the chatbot.
 * 
 * Built using the Scala Swing library, this module implements a modern,
 * dark-themed interface with glassmorphism effects and responsive layout.
 * 
 * UI Features:
 * - Event-Driven: Reactions to user clicks and text input.
 * - Dynamic Rendering: Auto-scrolling chat window and custom bubble components.
 * - Thread Safety: Use of SwingUtilities.invokeLater for asynchronous UI updates.
 */
object ModernKokiWokiUI extends SimpleSwingApplication {

  FlatDarkLaf.setup() // Apply the FlatLaf Look and Feel
  var currentUser = "" // Tracks the currently logged-in user
  var currentChatNum = 1 // Tracks the current session number for history saving

  // =====================================================
  // UI Components
  // =====================================================

  // chatPanel: Vertical container for all message bubbles
  val chatPanel = new BoxPanel(Orientation.Vertical) {
    opaque = false
    border = Swing.EmptyBorder(15, 20, 15, 20)
  }

  // scrollPane: Wraps the chatPanel to allow vertical scrolling
  val scrollPane = new ScrollPane(chatPanel) {
    opaque = false
    peer.getViewport.setBackground(Theme.bg1) // Use the theme background
    border = Swing.EmptyBorder(0, 0, 0, 0)
    verticalScrollBar.unitIncrement = 16 // Set scroll speed
  }

  // =====================================================
  // Main UI Frame
  // =====================================================

  def top = new MainFrame {
    title = "Koki Woki - AI Chef"
    minimumSize = new Dimension(950, 650)
    preferredSize = new Dimension(1000, 700)
    peer.setLocationRelativeTo(null) // Center window on screen

    // Main container with gradient background
    val contentContainer = new GradientPanel {
      // layout is automatically BorderPanel
    }

    // ── Sidebar: Contains logo, navigation buttons, and summary ───────────
    val sidebar = new BorderPanel {
      background = Theme.sidebarColor
      preferredSize = new Dimension(220, 0)
      border = Swing.EmptyBorder(20, 15, 20, 15)

      val titleLabel = new Label("👨‍🍳 Koki Woki") {
        foreground = Color.WHITE
        font = new Font("Segoe UI Emoji", Font.BOLD, 22)
      }

      val menuBox = new BoxPanel(Orientation.Vertical) {
        opaque = false
        contents += Swing.VStrut(30)
        
        // Summary Button: Shows conversation analytics
        val summaryBtn = Theme.createSidebarButton("🧠 Summary")
        summaryBtn.reactions += { case ButtonClicked(_) =>
          val history = chatBot.ui.Chatbot.getHistory
          val topics = chatBot.brain.ConversationBrain.extractTopics(history)
          val summaryText = s"Topics:\n${if (topics.nonEmpty) topics.mkString(", ") else "None"}\n\nMessages: ${history.size}"
          addMessage("Conversation Summary 🧠", summaryText, Theme.glass, false)
        }
        
        // Preferences Button: View and Reset user settings
        val prefBtn = Theme.createSidebarButton("⭐ Preferences")
        prefBtn.reactions += { case ButtonClicked(_) => 
          val prefs = ChatController.getPreferences(currentUser)
          val msg = if (prefs.isEmpty) "No preferences saved yet! 👨‍🍳" 
                    else prefs.map { case (k, v) => s"• $k: $v" }.mkString("\n")
          
          val res = Dialog.showConfirmation(null, s"Current Preferences:\n\n$msg\n\nWould you like to reset them?", "Your Preferences", Dialog.Options.YesNo)
          if (res == Dialog.Result.Yes) {
            PreferenceManager.clearPref(currentUser, "cuisine")
            PreferenceManager.clearPref(currentUser, "tag")
            PreferenceManager.clearPref(currentUser, "avoid")
            Dialog.showMessage(null, "Preferences cleared! 👨‍🍳", "Success")
          }
        }

        // History Button: Opens a dialog to view past messages
        val historyBtn = Theme.createSidebarButton("📜 History")
        historyBtn.reactions += { case ButtonClicked(_) =>
          val historyText = chatBot.ui.Chatbot.getHistory.zipWithIndex.map{ case (msg, i) => s"${i + 1}. $msg" }.mkString("\n\n")
          val area = new TextArea { editable=false; lineWrap=true; wordWrap=true; text=if(historyText.isEmpty) "No history" else historyText; foreground=Theme.textMain; background=Theme.bg1 }
          val dialog = new Dialog { title="History"; contents=new ScrollPane(area); size=new Dimension(400, 500); peer.setLocationRelativeTo(null) }
          dialog.open()
        }

        contents ++= Seq(summaryBtn, Swing.VStrut(15), prefBtn, Swing.VStrut(15), historyBtn)
      }

      val bottomBox = new BoxPanel(Orientation.Vertical) {
        opaque = false
        contents += Swing.VStrut(15)
        
        // Exit Button: Saves session and terminates app
        val exitBtn = Theme.createSidebarButton("🚪 Exit")
        exitBtn.reactions += { case ButtonClicked(_) => ChatController.saveSession(currentUser, currentChatNum); sys.exit(0) }
        contents += exitBtn
      }

      layout(titleLabel) = BorderPanel.Position.North
      layout(menuBox) = BorderPanel.Position.Center
      layout(bottomBox) = BorderPanel.Position.South
    }

    // ── Input Area: Where the user types messages ────────────────────────
    val inputField = new TextField {
      font = new Font("Segoe UI Emoji", Font.PLAIN, 16)
      background = Theme.glass
      foreground = Theme.textMain
      caret.color = Theme.textMain
      border = Swing.CompoundBorder(
        Swing.LineBorder(new Color(60, 65, 90), 1),
        Swing.EmptyBorder(12, 15, 12, 15)
      )
    }

    val sendBtn = new Button("Send") {
      font = new Font("Segoe UI", Font.BOLD, 14)
      background = Theme.userBubble
      foreground = Color.WHITE
      focusPainted = false
      preferredSize = new Dimension(100, 45)
    }

    val inputPanel = new BorderPanel {
      opaque = false
      border = Swing.EmptyBorder(20, 20, 20, 20)
      layout(inputField) = BorderPanel.Position.Center
      layout(sendBtn) = BorderPanel.Position.East
    }

    contentContainer.layout(sidebar) = BorderPanel.Position.West
    
    val centerWrapper = new BorderPanel {
      opaque = false
      layout(scrollPane) = BorderPanel.Position.Center
      layout(inputPanel) = BorderPanel.Position.South
    }
    
    contentContainer.layout(centerWrapper) = BorderPanel.Position.Center

    contents = contentContainer
    
    // Auto-save on window close event
    reactions += {
      case WindowClosing(_) =>
        if (currentUser.nonEmpty) ChatController.saveSession(currentUser, currentChatNum)
    }

    // ── Input Handling Logic ──────────────────────────────────────────────
    def processInput(): Unit = {
      val text = inputField.text.trim
      if (text.nonEmpty) {
        inputField.text = ""
        addMessage("You", s"💬 $text", Theme.userBubble, true) // Add user bubble

        val result = ChatController.processMessage(text, currentUser) // Get bot result
        val isAI = result.response.startsWith("✨ AI Chef Thinking ✨") || result.response.startsWith("✨ Challenge Accepted!")
        val bubbleColor = if (isAI) Theme.aiBubble else Theme.glass

        addMessage("Koki Woki 👨‍🍳", result.response, bubbleColor, false) // Add bot bubble

        // If there are specific suggestions, add them as a separate bubble
        if (result.recommendations.nonEmpty) {
          addMessage("Quick Picks 🥘", result.recommendations.map(r => s"• ${r.name}").mkString("\n"), Theme.glass, false)
        }
      }
    }

    sendBtn.reactions += { case ButtonClicked(_) => processInput() } // Handle click
    inputField.keys.reactions += { case KeyPressed(_, Key.Enter, _, _) => processInput() } // Handle Enter key
  }

  // =====================================================
  // Bubble Rendering: Creates custom rounded panels
  // =====================================================

  def createBubble(sender: String, message: String, color: Color, right: Boolean): Component = {
    val row = new BoxPanel(Orientation.Horizontal) { opaque = false }
    
    val textArea = new TextArea {
      text = message; editable = false; lineWrap = true; wordWrap = true
      foreground = Theme.textMain; background = color; opaque = false
      font = new Font("Segoe UI Emoji", Font.PLAIN, 15); border = Swing.EmptyBorder(0,0,0,0); columns = 24
    }

    val bubble = new RoundedPanel(color, 24) {
      opaque = false; border = Swing.EmptyBorder(14,18,14,18)
      contents += new Label(sender) { foreground = Theme.textMain; font = new Font("Segoe UI Emoji", Font.BOLD, 15) }
      contents += Swing.VStrut(6)
      contents += textArea
      maximumSize = preferredSize
    }

    // Align right for user, left for bot
    if (right) { row.contents += Swing.HGlue; row.contents += bubble } 
    else { row.contents += bubble; row.contents += Swing.HGlue }
    row.border = Swing.EmptyBorder(4,4,4,4)
    row
  }

  // addMessage: Adds a bubble and triggers auto-scroll
  def addMessage(sender: String, message: String, color: Color, right: Boolean = false): Unit = {
    chatPanel.contents += createBubble(sender, message, color, right)
    chatPanel.contents += Swing.VStrut(14)
    chatPanel.revalidate()
    chatPanel.repaint()
    
    // Asynchronous UI update to scroll to bottom after layout calculation
    javax.swing.SwingUtilities.invokeLater(() => {
      val vertical = scrollPane.peer.getVerticalScrollBar
      vertical.setValue(vertical.getMaximum)
    })
  }

  // =====================================================
  // Login Dialog: Handles Auth and Session Picking
  // =====================================================

  def showLoginDialog(): Boolean = {
    val userField = new JTextField(15)
    val passField = new JPasswordField(15)
    val panel = new JPanel(new java.awt.GridLayout(2, 2, 10, 10))
    panel.add(new JLabel("Username")); panel.add(userField)
    panel.add(new JLabel("Password")); panel.add(passField)

    val res = JOptionPane.showConfirmDialog(null, panel, "Koki Woki — Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
    if (res != JOptionPane.OK_OPTION) sys.exit(0)

    val u = userField.getText.trim
    val p = passField.getPassword.mkString.trim
    if (u.isEmpty || p.isEmpty) { JOptionPane.showMessageDialog(null, "Cannot be empty."); return false }

    if (ChatController.userExists(u)) {
      if (ChatController.login(u, p)) {
        currentUser = u
        val summaries = ChatController.getSummaries(u)
        if (summaries.nonEmpty) {
          val options = "New conversation" :: summaries.zipWithIndex.map{ case (s, i) => s"${i+1}. $s" }
          val combo = new JComboBox(options.toArray)
          val res = JOptionPane.showConfirmDialog(null, combo, s"Welcome back, $u!\nPick a past session.", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)
          
          if (res == JOptionPane.OK_OPTION) {
            val choice = combo.getSelectedIndex
            if (choice > 0) {
              currentChatNum = choice
              val loaded = ChatController.loadSession(u, choice)
              loaded.foreach { entry =>
                addMessage("You", s"💬 ${entry.userInput}", Theme.userBubble, true)
                addMessage("Koki Woki 👨‍🍳", entry.botResponse, Theme.glass, false)
              }
            } else {
              currentChatNum = summaries.size + 1
            }
          } else {
            sys.exit(0) // User cancelled session pick
          }
        } else {
          currentChatNum = 1
        }
        true
      } else { JOptionPane.showMessageDialog(null, "Wrong password."); false }
    } else {
      // Automatic Registration Flow
      if (JOptionPane.showConfirmDialog(null, s"User '$u' not found.\nRegister?", "Register", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        ChatController.register(u, p)
        JOptionPane.showMessageDialog(null, "Account created! Please log in.")
      }
      false
    }
  }

  // =====================================================
  // App Startup
  // =====================================================

  override def startup(args: Array[String]): Unit = {
    while (!showLoginDialog()) {} // Loop until successful login
    top.visible = true // Show main window
    addMessage("Koki Woki 👨‍🍳", s"Welcome to Koki Woki, Chef $currentUser! 👨‍🍳\n\nI can help you find recipes, create quick meals, or even invent something new!\n\nJust say 'hello' to get started.", Theme.glass, false)
  }
}

// ===============================================
// UI THEME & CUSTOM COMPONENTS DEFINITIONS
// ===============================================

/** RoundedPanel: A custom panel that paints a rounded rectangle background. */
class RoundedPanel(color: Color, radius: Int = 22) extends BoxPanel(Orientation.Vertical) {
  opaque = false
  override def paintComponent(g: Graphics2D): Unit = {
    val g2 = g.create().asInstanceOf[Graphics2D]
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(color)
    g2.fillRoundRect(0, 0, size.width, size.height, radius, radius)
    g2.dispose()
    super.paintComponent(g)
  }
}

/** GradientPanel: A custom panel that paints a vertical gradient background. */
class GradientPanel extends BorderPanel {
  opaque = false
  override def paintComponent(g: Graphics2D): Unit = {
    super.paintComponent(g)
    val g2 = g.create().asInstanceOf[Graphics2D]
    val gradient = new GradientPaint(0, 0, Theme.bg1, size.width, size.height, Theme.bg2)
    g2.setPaint(gradient)
    g2.fillRect(0, 0, size.width, size.height)
    g2.dispose()
  }
}

/** Theme: Central object for colors, fonts, and shared UI assets. */
object Theme {
  val bg1: Color = new Color(10, 12, 24)
  val bg2: Color = new Color(20, 24, 40)
  val sidebarColor: Color = new Color(18, 22, 36)
  
  val glass: Color = new Color(32, 36, 55, 225)
  val userBubble: Color = new Color(88, 101, 242, 230)
  val aiBubble: Color = new Color(45, 38, 20, 235)

  val textMain: Color = new Color(245, 247, 255)
  val textSoft: Color = new Color(180, 188, 208)

  /** Helper to create consistent sidebar buttons with theme styling. */
  def createSidebarButton(textStr: String): Button = {
    new Button(textStr) {
      background = glass
      foreground = Color.WHITE
      font = new Font("Segoe UI Emoji", Font.BOLD, 16)
      focusPainted = false
      border = Swing.EmptyBorder(12, 18, 12, 18)
      maximumSize = new Dimension(170, 55)
      xLayoutAlignment = 0.0
    }
  }
}