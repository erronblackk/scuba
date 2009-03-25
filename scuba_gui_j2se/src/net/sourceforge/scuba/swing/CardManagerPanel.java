package net.sourceforge.scuba.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.CardTerminal;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.scuba.smartcards.CardEvent;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardTerminalListener;
import net.sourceforge.scuba.smartcards.TerminalCardService;
import net.sourceforge.scuba.util.Icons;

/**
 * A generic card manager GUI component. Will probably be removed soon.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 *
 * @deprecated Gone
 */
public class CardManagerPanel extends JPanel
{
	private static final long serialVersionUID = 8988528742159123032L;

	private static final Font UNSELECTED_FONT = new Font("Sans-serif", Font.PLAIN, 12);
	private static final Font SELECTED_FONT = new Font("Sans-serif", Font.PLAIN, 12);

	private static final Color UNSELECTED_COLOR = Color.BLACK;
	private static final Color SELECTED_COLOR = Color.RED;

	private static final Icon CM_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("computer"));
	private static final Icon TERMINAL_NOT_POLLING_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive"));
	private static final Icon TERMINAL_NO_CARD_ICON = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_delete"));
	private static final Icon TERMINAL_OTHER_CARD_ICON  = new ImageIcon(Icons.getFamFamFamSilkIcon("drive_add"));

	private JTree tree;

	private List<CardTerminal> terminals;
	private List<TerminalNode> terminalNodes;

	public CardManagerPanel() {
		super(new BorderLayout());
		final CardManager cm = CardManager.getInstance();
		JPanel northPanel = new JPanel(new FlowLayout());
		JCheckBox cardManagerCheckBox = new JCheckBox();
		northPanel.add(cardManagerCheckBox);
		add(northPanel, BorderLayout.NORTH);
		terminalNodes = new ArrayList<TerminalNode>();
		terminals = cm.getTerminals();
		for (CardTerminal terminal: terminals) {
			TerminalNode node = new TerminalNode(terminal);
			terminalNodes.add(node);
		}
		tree = new JTree(buildTree(cm));
		TreeCellRenderer renderer = new DefaultTreeCellRenderer() {

			private static final long serialVersionUID = 131460608635144777L;

			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean selected, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {

				if (value instanceof TerminalNode) {
					TerminalNode node = (TerminalNode)value;
					CardTerminal terminal = node.getTerminal();
					JLabel label = new JLabel(terminal.getName(), node.getIcon(), JLabel.LEFT);
					label.setFont(selected ? SELECTED_FONT : UNSELECTED_FONT);
					label.setForeground(selected ? SELECTED_COLOR : UNSELECTED_COLOR);
					return label;
				} else {
					/* Must be root */
					JLabel label = new JLabel(value.toString(), CM_ICON, JLabel.LEFT);
					label.setFont(selected ? SELECTED_FONT : UNSELECTED_FONT);
					label.setForeground(selected ? SELECTED_COLOR : UNSELECTED_COLOR);
					return label;
				}
			}
		};
		tree.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				CardTerminal terminal = getTerminal(e);
				if (terminal == null) { return; }
				if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
					System.out.println("DEBUG: left double clicked on " + terminal.getName());
				} 
			}

			public void mousePressed(MouseEvent e) {
				CardTerminal terminal = getTerminal(e);
				if (terminal == null) { return; }
				if (e.getButton() != MouseEvent.BUTTON3) { return; }
				JPopupMenu popup = getPopupMenu(terminal);
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
			
			private CardTerminal getTerminal(MouseEvent e) {
				int x = e.getX(),y = e.getY();
				TreePath selPath = tree.getPathForLocation(x, y);
				if (selPath == null) { return null; }
				Object obj = selPath.getLastPathComponent();
				if (!(obj instanceof TerminalNode)) { return null; }
				return ((TerminalNode)obj).getTerminal();
			}
		});
		tree.setCellRenderer(renderer);
		add(new JScrollPane(tree), BorderLayout.CENTER);
		cm.addCardTerminalListener(new CardTerminalListener() {
			public void cardInserted(CardEvent ce) {
				CardService service = ce.getService();
				if (service instanceof TerminalCardService) {
					CardTerminal terminal = ((TerminalCardService)service).getTerminal();
					TerminalNode node = terminalNodes.get(terminals.indexOf(terminal));
					Icon icon = node.getIcon();
					if (icon == TERMINAL_NOT_POLLING_ICON || node.getIcon() == TERMINAL_NO_CARD_ICON) {
						node.setIcon(TERMINAL_OTHER_CARD_ICON);
					}
					revalidate();
				}
			}

			public void cardRemoved(CardEvent ce) {
				CardService service = ce.getService();
				if (service instanceof TerminalCardService) {
					CardTerminal terminal = ((TerminalCardService)service).getTerminal();
					TerminalNode node = terminalNodes.get(terminals.indexOf(terminal));
					node.setIcon(TERMINAL_NO_CARD_ICON);
					revalidate();
				}
			}			
		});
	}

	private TreeNode buildTree(CardManager cm) {
		return new DefaultMutableTreeNode("Card Manager", true) {

			private static final long serialVersionUID = 193297024953497869L;

			public int getChildCount() {
				return terminalNodes.size();
			}

			public TreeNode getChildAt(int childIndex) {
				return terminalNodes.get(childIndex);
			}
		};
	}

	private JPopupMenu getPopupMenu(CardTerminal terminal) {
		JPopupMenu menu = new JPopupMenu();
		JMenuItem readPassportItem = new JMenuItem();
		menu.add(readPassportItem);
		JMenuItem guessCountryItem = new JMenuItem();
		menu.add(guessCountryItem);
		return menu;
	}

	private class TerminalNode extends DefaultMutableTreeNode
	{
		private static final long serialVersionUID = -5808106190717549930L;

		CardTerminal terminal;
		Icon icon;

		public TerminalNode(CardTerminal terminal) {
			this.terminal = terminal;
			if (CardManager.getInstance().isPolling()) {
				this.icon = TERMINAL_NO_CARD_ICON;
			} else {
				this.icon = TERMINAL_NOT_POLLING_ICON;
			}
		}

		public void setIcon(Icon icon) {
			this.icon = icon;
		}

		public Icon getIcon() {
			return icon;
		}

		public String toString() {
			return terminal.getName();
		}

		public CardTerminal getTerminal() {
			return terminal;
		}
	}

	public void revalidate() {
		super.revalidate();
		if (tree != null) { tree.repaint(); }
		super.revalidate();
		repaint();
	}
}