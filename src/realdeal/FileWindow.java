/**
 *
 * 
 *
 */

package realdeal;

import assorted.Stack;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import prodata.DtreeBeta;

/**
 * All files without permission to read are completely blocked from model.
 * Hidden files are only blocked if showhidden set to false.
 *
 * @author chris
 */
public class FileWindow extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

    //File current;
    DtreeBeta<String, Fnode> ftree;
    private static final int DIR_SIZE = 18;//font size for directory
    private static final int FILE_SIZE = 16;//font size for file
    private static Font DIR_FONT = new Font("SansSerif.plain", Font.BOLD, DIR_SIZE);
    private static Font FILE_FONT = new Font("SansSerif.plain", Font.PLAIN, FILE_SIZE);
    private static final int DIR_ASCENT = 17;//are obtained from FontMetrics
    private static final int DIR_DESCENT = 5;
    private static final int FILE_ASCENT = 15;
    private static final int FILE_DESCENT = 4;
    int px, py;//current upper left location
    int maxx, maxy;//conceptual bounds, not view window bounds
    int cwidth, cheight;//dims of view port
    int scrollspeed = 15;//pixels to mouse wheel click(which is indirectly speed)
    int gap = 5;//controls spacing between
    int vstart = 5;//beginning of text section
    boolean showhidden;
    
    TextEditor base;

    public FileWindow(TextEditor ba) {
        this(ba, "/");
    }

    public FileWindow(TextEditor ba, String start) {
        base = ba;
        load(start);
        addMouseWheelListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    /**
     * Builds the model of file tree starting at fileName.
     * 
     */
    private void load(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            ftree = new DtreeBeta<>(fileName, new Fnode(file, true));//default to open
            if (file.isDirectory()) {
                File[] desc = file.listFiles();//only returns null if not a directory
                for (int f = 0; f < desc.length; f++) {
                    if (desc[f].canRead() && (!desc[f].isHidden() || showhidden)) {//completely block anything without permission to read and hiddens if no hiddens
                        ftree.extend(fileName, desc[f].getName(), new Fnode(desc[f]));
                    }
                }
            }
            setMax();
            repaint();
        } else {
            throw new IllegalArgumentException("Invalid File. Aborting.");
        }
    }
    //to be fired by Up button.
    private void up(){
        Fnode head = ftree.getRoot();
        File parent = null;
        if((parent = head.data.getParentFile()) != null){
            //a parent directory exists
            load(parent.getName());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); //Clears panel
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cwidth = getWidth();
        cheight = getHeight();
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, cwidth, cheight);
        //draw dtree
        g2.setColor(Color.black);
        if (ftree != null) {
            drawFileTree(g2);
        }
    }
    boolean debug = false;

    /**
     * Draw Algo: Depth-First Traversal.
     *
     * @param g
     */
    private void drawFileTree(Graphics2D g) {//most params are here cuz static!, DtreeBeta<String, Fnode> tree, int cx, int cy, int gap
        if (ftree == null) {
            return;
        }
        int vert = vstart;//will be vstart
        int offset = 10;//horiz
        Stack<String> frontier = new Stack<>();
        frontier.push(ftree.getRootKey());
        String cursor = null;
        Fnode fcursor = null;
        while (frontier.isNotEmpty()) {
            cursor = frontier.pop();
            fcursor = ftree.get(cursor);
            offset = 10 + (10 * ftree.getDepth(cursor));//initial + 10*depth
            g.setFont(fcursor.directory ? DIR_FONT : FILE_FONT);
            int voff = (fcursor.directory ? DIR_ASCENT : FILE_ASCENT);
            g.drawString(cursor, offset, (vert + gap + voff) - py);//minus py handles scrolling.
            vert = vert + gap + voff + (fcursor.directory ? DIR_DESCENT : FILE_DESCENT);
            if (fcursor.directory && fcursor.open && ftree.getChildCount(cursor) > 0) {
                String[] children = ftree.getChildren(cursor);
                for (int c = 0; c < children.length; c++) {
                    frontier.push(children[c]);
                }
            }
        }
    }

    private void performDebug(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics(FILE_FONT);
        System.out.println("dir font size: " + FILE_SIZE);
        System.out.println("height: " + fm.getHeight());
        System.out.println("ascent: " + fm.getAscent());
        System.out.println("descent: " + fm.getDescent());
        System.out.println("leading: " + fm.getLeading());
    }

    public void setHidden(boolean showHidden) {
        showhidden = showHidden;
        repaint();
    }

    //all algos need a stack, so keep a utility stack created.
    Stack<String> stack = new Stack<>();

    private String itemAt(int mx, int my) {//mx,my expected to be in objectSpace, not CamSpace.
        if (my < vstart || my > maxy) {
            return null;
        }
        stack.clear();
        int depth = vstart;
        stack.push(ftree.getRootKey());
        String cursor = null;
        Fnode fcursor = null;
        while (stack.isNotEmpty()) {
            cursor = stack.pop();
            fcursor = ftree.get(cursor);
            depth = depth + gap + (fcursor.directory ? DIR_ASCENT + DIR_DESCENT : FILE_ASCENT + FILE_DESCENT);//top of next item
            if (depth + (0.5 * gap) >= my) {
                return cursor;//reach halfway into gap
            }
            if (fcursor.directory && fcursor.open && ftree.getChildCount(cursor) > 0) {
                String[] children = ftree.getChildren(cursor);
                for (int c = 0; c < children.length; c++) {
                    stack.push(children[c]);
                }
            }
        }
        return null;
    }

    private void setMax() {
        stack.clear();
        int depth = vstart;
        stack.push(ftree.getRootKey());
        String cursor = null;
        Fnode fcursor = null;
        while (stack.isNotEmpty()) {
            cursor = stack.pop();
            fcursor = ftree.get(cursor);
            depth = depth + gap + (fcursor.directory ? DIR_ASCENT + DIR_DESCENT : FILE_ASCENT + FILE_DESCENT);//top of next item
            if (fcursor.directory && fcursor.open && ftree.getChildCount(cursor) > 0) {
                String[] children = ftree.getChildren(cursor);
                for (int c = 0; c < children.length; c++) {
                    stack.push(children[c]);
                }
            }
        }
        maxy = depth;
    }
    private void getDirectory(String id){
        Fnode fn = ftree.get(id);
        if(fn == null) return;
        if(ftree.hasParent(id)){
            Fnode parent = ftree.get(ftree.getParentKey(id));
            
        }
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        if ((me.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {//Left
            if (me.getClickCount() >= 2) {
                int rx = px + me.getX();
                int ry = py + me.getY();
                String snag = null;
                if ((snag = itemAt(rx, ry)) != null) {
                    //System.out.println("Double-clicked dir: " + snag);
                    Fnode fn = ftree.get(snag);
                    if (fn.directory) {
                        fn.open = !fn.open;
                        if (ftree.getChildCount(snag) == 0) {//screens case where already loaded
                            File[] desc = fn.data.listFiles();
                            for (int f = 0; f < desc.length; f++) {//fail-fast for nothing
                                if (desc[f].canRead() && (!desc[f].isHidden() || showhidden)) {//completely block anything without permission to read
                                    ftree.extend(snag, desc[f].getName(), new Fnode(desc[f]));
                                }
                            }
                        }
                        setMax();
                        repaint();
                    } else {
                        //double clicked file
                        base.openFile(fn.data);
                    }
                }
            } else {//Single-click Left
                //System.out.println("Left");
            }
        } else if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {//Right
            System.out.println("Right");
        }
    }

    @Override
    public void mousePressed(MouseEvent me) {

    }

    @Override
    public void mouseReleased(MouseEvent me) {

    }

    @Override
    public void mouseEntered(MouseEvent me) {

    }

    @Override
    public void mouseExited(MouseEvent me) {

    }

    @Override
    public void mouseDragged(MouseEvent me) {

    }

    @Override
    public void mouseMoved(MouseEvent me) {

    }

    /**
     * Establishes 1 scrollspeed of pixels to each mouse wheel tick. Scrolling
     * down produces 1, scrolling up produces -1
     *
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent me) {
        int ticks = me.getWheelRotation();
        if (ticks == 1 && (py + cheight < maxy - scrollspeed + 1)) {//scroll down
            py += scrollspeed;
            repaint();
        } else if (ticks == -1 && (py > 0 + scrollspeed - 1)) {
            py -= scrollspeed;
            repaint();
        }
    }
    /*
     *  ScrollBlock Algo allows us to set any
     *  unit of progression and still detect border. Flawless!
     */

    public void scrollDown() {
        if (py + cheight < maxy - scrollspeed + 1) {
            py += scrollspeed;
            repaint();
        }
    }

    public void scrollUp() {//for mousewheel
        if (py > 0 + scrollspeed - 1) {
            py -= scrollspeed;
            repaint();
        }
    }

    public void scrollEast() {
        if (px > 0 + scrollspeed - 1) {
            px -= scrollspeed;
            repaint();
        }
    }

    public void scrollWest() {
        if (px + cwidth < maxx - scrollspeed + 1) {
            px += scrollspeed;
            repaint();
        }
    }

    static class Fnode {

        File data;
        boolean directory;
        boolean open;

        public Fnode(File f) {
            data = f;
            directory = data.isDirectory();
            //hidden = data.isHidden();
        }

        public Fnode(File f, boolean isOpen) {
            data = f;
            open = isOpen;
            directory = data.isDirectory();
            //hidden = data.isHidden();
        }
    }

    private JPopupMenu getPopup() {
        JPopupMenu pop = new JPopupMenu("Right");
        //attach a listener
        JMenuItem adjust = new JMenuItem("Adjust");
        //adjust.addActionListener(menuListener);
        pop.add(adjust);

        return pop;
    }

    static void showAvailableFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = ge.getAllFonts();
        for (Font f : fonts) {
            System.out.println(f.getName());
        }
    }

    public static void main(String[] args) {
        //FileWindow fw = new FileWindow();
        //fw.load("/");
        File home = new File("/");
        File[] dirs = home.listFiles();
        for (int d = 0; d < dirs.length; d++) {
            System.out.print(dirs[d] + "= ");
            System.out.print("Hidden: " + dirs[d].isHidden());
            System.out.println(", Can Read: " + dirs[d].canRead());
        }
    }
}
