package astah.plugin

import com.change_vision.jude.api.inf.ui.ISelectionListener
import com.change_vision.jude.api.inf.project.ProjectEvent
import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.model.IDiagram
import com.change_vision.jude.api.inf.project.ProjectEventListener
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionEvent
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionListener
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.DefaultTableColumnModel
import java.awt.Dimension
import java.rmi.server.UID
import javax.swing.JTextField
import javax.swing.table.DefaultTableCellRenderer

class TicketsTabView : JPanel(), IPluginExtraTabView, ProjectEventListener, ActionListener,
        IDiagramEditorSelectionListener {
    val LABEL_WIDTH = 150
    val ROW_HEIGHT = 22
    val textFieldRedmineUri = JTextField("(input the redmine uri)", 50)
    val textFieldRedmineKey = JTextField("(input the redmine api access key)", 50)
    val textFieldRedminePrj = JTextField("(input the redmine project id)", 50)
    val buttonSetupRedmine = JButton("[Setup Redmine]")
    val buttonAddTicket = JButton("[Add Ticket]")
    val buttonAllTickets = JButton("[All Tcikets]")
    val buttonSyncFromRedmine = JButton("[Sync: astah* <- Redmine]")
    val buttonRedmineInformation = JButton(" [Redmine Information]")
    val buttonPasteUri = JButton("Paste")
    val buttonPasteKey = JButton("Paste")
    val buttonPastePrj = JButton("Paste")
    val beforeSetupPanel = Panel()
    val afterSetupBar = JToolBar()
    val table : JTable
    var tableModel : DefaultTableModel
    val columnModel : DefaultTableColumnModel
    val scroller : JScrollPane
    val displayedTickets = mutableListOf<Ticket>()
    val sortDirections = mutableMapOf<Int, Boolean>()
    // val highestPriorityColor = Color(247, 199, 198)
    // val lowestPriorityColor = Color(236, 247, 254)
    init {
        // init table
        tableModel = object : DefaultTableModel() {
            override fun isCellEditable(rowIndex : Int, columnIndex : Int) : Boolean = false
        }
        Column.values().forEach { tableModel.addColumn(it.text) }
        table = JTable(tableModel)
        with(table) {
            setRowHeight(ROW_HEIGHT)
            setSurrendersFocusOnKeystroke(true)
            setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e : MouseEvent) = clickMouseTable(e)
            })
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e : KeyEvent) = keyPressedTable(e)
            })
        }
        table.tableHeader.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e : MouseEvent) = clickMouseTableHeader(e)
        })
        columnModel = table.columnModel as DefaultTableColumnModel
        Column.values().forEach { columnModel.getColumn(it.index).preferredWidth = it.defaultWidth }
        val renderer = DefaultTableCellRenderer()
        renderer.horizontalAlignment = SwingConstants.RIGHT
        columnModel.getColumn(Column.TICKET_NUMBER.index).setCellRenderer(renderer)
        columnModel.getColumn(Column.TICKET_ESTIMATED_HUORS.index).setCellRenderer(renderer)
        columnModel.getColumn(Column.TICKET_DONE_RATIO.index).setCellRenderer(renderer)
        scroller = JScrollPane(table)

        // init buttons
        initButton()

        // initPanel
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val bar0BeforeSetup = JToolBar()
        val bar1BeforeSetup = JToolBar()
        val bar2BeforeSetup = JToolBar()
        val bar3BeforeSetup = JToolBar()
        bar0BeforeSetup.add(buttonSetupRedmine)

        // Redmine URI Information Input
        val uriLabel = JLabel("Redmine URI: ")
        uriLabel.preferredSize = Dimension(LABEL_WIDTH, ROW_HEIGHT)
        bar1BeforeSetup.add(uriLabel)
        buttonPasteUri.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = textFieldRedmineUri.paste()
        })
        bar1BeforeSetup.add(textFieldRedmineUri)
        bar1BeforeSetup.add(buttonPasteUri)

        // Redmine Access Key Information Input
        val keyLabel = JLabel("API access key: ")
        keyLabel.preferredSize = Dimension(LABEL_WIDTH, ROW_HEIGHT)
        bar2BeforeSetup.add(keyLabel)
        buttonPasteKey.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e : MouseEvent) = textFieldRedmineKey.paste()
        })
        bar2BeforeSetup.add(textFieldRedmineKey)
        bar2BeforeSetup.add(buttonPasteKey)

        // Redmine Project Information Input
        val prjLabel = JLabel("Redmine project name: ")
        prjLabel.preferredSize = Dimension(LABEL_WIDTH, ROW_HEIGHT)
        bar3BeforeSetup.add(prjLabel)
        buttonPastePrj.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e : MouseEvent) = textFieldRedminePrj.paste()
        })

        bar3BeforeSetup.add(textFieldRedminePrj)
        bar3BeforeSetup.add(buttonPastePrj)
        beforeSetupPanel.layout = GridLayout(4,1)
        beforeSetupPanel.add(bar1BeforeSetup)
        beforeSetupPanel.add(bar2BeforeSetup)
        beforeSetupPanel.add(bar3BeforeSetup)
        beforeSetupPanel.add(bar0BeforeSetup)
        afterSetupBar.add(buttonAddTicket)
        afterSetupBar.add(buttonAllTickets)
        afterSetupBar.add(buttonSyncFromRedmine)
        afterSetupBar.add(buttonRedmineInformation)
        add(beforeSetupPanel)
        add(scroller)
        addEventListeners()
        Column.values().forEach { sortDirections.put(it.index, false) }
    }
    fun initButton() {
        buttonSetupRedmine.addActionListener(this)
        buttonAddTicket.addActionListener(this)
        buttonAllTickets.addActionListener(this)
        buttonSyncFromRedmine.addActionListener(this)
        buttonRedmineInformation.addActionListener(this)
    }
    fun addEventListeners() {
        try {
            val projectAccessor = AstahAPI.getAstahAPI().projectAccessor
            val diagramViewManager = projectAccessor.viewManager.diagramViewManager
            projectAccessor.addProjectEventListener(this)
            diagramViewManager.addDiagramEditorSelectionListener(this)
        } catch (e : ClassNotFoundException) {
            e.message
        }
    }
    fun setupRedmine() {
        val url = textFieldRedmineUri.getText().trim()
        val key = textFieldRedmineKey.getText().trim()
        val prj = textFieldRedminePrj.getText().trim()
        if (TicketsManager.setupRedmine(url, key, prj, AstahAccessor.getCurrentProjectName())) {
            showAfterPanel()
            TicketsManager.loadFromRedmine()
            update()
        } else {
            JOptionPane.showMessageDialog(null,
                    "The redmine in the text fields cannot be found.")
        }
    }
    fun addTicket() {
        val currentDiagram = AstahAccessor.getCurrentDiagram()
        if (currentDiagram == null) {
            JOptionPane.showMessageDialog(null, "Select any diagram.")
        } else {
            val ticket = Ticket(UID().toString(), AstahAccessor.getCurrentProjectName(),
                    currentDiagram.name, AstahAccessor.getNamespace(currentDiagram), currentDiagram.id)
            TicketsManager.addTicket(ticket)
            saveTableWidthsToProject()
            update()
        }
    }
    fun saveTableWidthsToProject() {
        val widths = mutableListOf<ColumnWidth>()
        Column.values().forEach { widths.add(ColumnWidth(it.index, columnModel.getColumn(it.index).width)) }
        AstahAccessor.writeTaggedValue(AstahTag.REDMINE_TABLE_COLUMN_WIDTHS.key,
                JsonSaveDataConverter.convertFromWidthsToJSON(widths.toTypedArray()))
    }
    fun loadTableWidthsFromProject() {
        val savedWidths = AstahAccessor.readTaggedValue(AstahTag.REDMINE_TABLE_COLUMN_WIDTHS.key)
        if (savedWidths != null) {
            val columnWidths = JsonSaveDataConverter.convertFromJsonToWidths(savedWidths)
            columnWidths.forEach { columnModel.getColumn(it.index).preferredWidth = it.width }
        }
    }
    fun showBeforePanel() {
        remove(afterSetupBar)
        remove(scroller)
        add(beforeSetupPanel)
        add(scroller)
    }
    fun showAfterPanel() {
        remove(beforeSetupPanel)
        remove(scroller)
        add(afterSetupBar)
        add(scroller)
    }
    override fun actionPerformed(e : ActionEvent) {
        when (e.source) {
            buttonSetupRedmine -> setupRedmine()
            buttonAddTicket -> addTicket()
            buttonAllTickets -> update { (_) -> true }
            buttonSyncFromRedmine -> {
                TicketsManager.loadFromRedmine()
                saveTableWidthsToProject()
                update()
            }
            buttonRedmineInformation -> {
                textFieldRedmineUri.text = RedmineAccessor.redmineUri
                textFieldRedmineKey.text = RedmineAccessor.redmineKey
                textFieldRedminePrj.text = RedmineAccessor.redminePrj
                showBeforePanel()
            }
        }
    }
    override fun diagramSelectionChanged(p0 : IDiagramEditorSelectionEvent?) {
        val currentDiagram = AstahAccessor.getCurrentDiagram()
        if (currentDiagram != null)
            update(currentDiagram)
    }
    override fun projectChanged(e : ProjectEvent?) {
        TicketsManager.updateFromProject()
        update()
    }
    override fun projectClosed(e : ProjectEvent?) {
        TicketsManager.closeAllTickets()
        showBeforePanel()
        update()
    }
    override fun projectOpened(e : ProjectEvent?) {
        if (TicketsManager.loadFromProject()) {
            showAfterPanel()
            loadTableWidthsFromProject()
            update()
        } else if (!AstahAccessor.isNewlyCreatedProject()) {
            textFieldRedmineUri.text = RedmineAccessor.redmineUri
            textFieldRedmineKey.text = RedmineAccessor.redmineKey
            textFieldRedminePrj.text = RedmineAccessor.redminePrj
            revalidate()
        }
    }
    override fun addSelectionListener(listener : ISelectionListener) = Unit
    override fun getComponent() : Component = this
    override fun getDescription() : String = "Redmine Plugin View"
    override fun getTitle() : String = "Redmine Tickets"
    override fun activated() = Unit
    override fun deactivated() = Unit
    fun clickMouseTable(e : MouseEvent) {
        if (e.button == MouseEvent.BUTTON1) {
            val idx = table.rowAtPoint(e.point)
            val idy = table.columnAtPoint(e.point)
            when (idy) {
                Column.TICKET_SUBJECT.index, Column.TICKET_NUMBER.index ->
                    RedmineAccessor.openTicketInBrowser(table.getValueAt(idx, Column.TICKET_NUMBER.index) as String)
                Column.DIAGRAM_NAME.index, Column.DIAGRAM_NAMESPACE.index ->
                    AstahAccessor.selectDiagram(table.getValueAt(idx, Column.DIAGRAM_ID.index) as String)
            }
            table.requestFocus()
        }
    }
    fun clickMouseTableHeader(e : MouseEvent) {
        if (e.button == MouseEvent.BUTTON1) {
            val idy = table.columnAtPoint(e.point)
            when (Column.values()[idy]) {
                Column.TICKET_PRIORITY -> {
                    val att0 = Column.values()[idy].att as (Ticket) -> Priority
                    val att : (Ticket) -> Int = { att0(it).id }
                    sort(sortDirections[idy]!!, att)
                }
                Column.TICKET_NUMBER, Column.TICKET_DONE_RATIO -> {
                    val att = Column.values()[idy].att as (Ticket) -> Int
                    sort(sortDirections[idy]!!, att)
                }
                Column.TICKET_ESTIMATED_HUORS -> {
                    val att = Column.values()[idy].att as (Ticket) -> Float
                    sort(sortDirections[idy]!!, att)
                }
                else -> {
                    val att = Column.values()[idy].att as (Ticket) -> String
                    sort(sortDirections[idy]!!, att)
                }
            }
            sortDirections.replace(idy, !sortDirections[idy]!!)
            updateTable()
        }
    }
    fun <R : Comparable<R>> sort(direction : Boolean, selector : (Ticket) -> R?) {
        if (direction) displayedTickets.sortByDescending(selector) else displayedTickets.sortBy(selector)
    }
    fun keyPressedTable(e : KeyEvent) {
    }
    fun update() {
        val currentDiagram = AstahAccessor.getCurrentDiagram()
        if (currentDiagram == null) update(fun (t) = true) else update(currentDiagram)
    }
    fun update(d : IDiagram) {
        update(fun (t) = t.diagramId == d.id)
    }
    fun update(f : (Ticket) -> Boolean) {
        displayedTickets.clear()
        displayedTickets.addAll(TicketsManager.tickets.filter(f))
        updateTable()
    }
    fun updateTable() {
        tableModel.rowCount = displayedTickets.size
        var i = 0
        displayedTickets.forEach { ticket ->
            Column.values().forEach {
                if (it == Column.TICKET_PRIORITY)
                    table.setValueAt(ticket.priority.text, i, it.index)
                else
                    table.setValueAt(it.att(ticket).toString(), i, it.index)
            }
            i++
        }
        revalidate()
    }
}