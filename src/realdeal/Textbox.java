/**
 *
 *
 *
 */
package realdeal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 *
 * @author chris
 */
public class Textbox extends JPanel {
    
    private static final Font STANDARD_FONT = new Font("SansSerif", Font.PLAIN, 14);
    Font activeFont = STANDARD_FONT;
    public static final int DEF_COLOR = 0x00000000;
    int baseColor = DEF_COLOR;
    
    boolean activated;
    boolean initialized;
    boolean isEditable;
    
    JPopupMenu rightClick;
    
    //SubSystems
    CaretSys caret;
    ScrollSys scroll;
    HighlightSys highlight;
    LayoutSys layout;
    Metrics metrics;
    
    Model model;
    Editor editor;
    
    //Core Control
    Processor core;
    Lock modelLock = new Lock();
    Handler handler = new Handler();
    
    /*
    Defaults: Non-Editable, No word-wrap
    */

    public Textbox() {
        setup(null);
    }
    public Textbox(String text){
        setup(null);
        layout.initModel(Metrics.harvest(text));
    }
    public Textbox(File file){
        setup(null);
        layout.initModel(Metrics.harvest(file));
    }
    public Textbox(Editor ed){
        isEditable = true;
        setup(ed);
    }
    public Textbox(Editor ed, File file){
        isEditable = true;
        setup(ed);
        layout.initModel(Metrics.harvest(file));
    }
    private void setup(Editor ed){
        core = new Processor();
        core.start();//Ignites worker thread.
        
        //rightClick = createMenu();
        metrics = new Metrics(activeFont);
        model = new Model();
        model.setDefaultColor(baseColor);
        
        rightClick = createMenu();
        //Swing Layout: Absolute Position
        this.setLayout(null);
        //Create Subsystems
        layout = new LayoutSys();
        scroll = new ScrollSys();
        highlight = new HighlightSys();
        caret = new CaretSys(this);
        //Load dependencies
        layout.scroll = scroll;
        layout.model = model;
        layout.metrics = metrics;
        
        scroll.layout = layout;
        scroll.model = model;
        scroll.metrics = metrics;
        
        highlight.model = model;
        highlight.layout = layout;
        highlight.caret = caret;
        highlight.scroll = scroll;
        highlight.metrics = metrics;
        
        caret.model = model;
        caret.metrics = metrics;
        caret.scroll = scroll;
        caret.layout = layout;
        
        if(isEditable){
            editor = ed != null ? ed : new BasicEditor();
            editor.caret = caret;
            editor.setModel(model);
            editor.highlight = highlight;
            addKeyListener(handler);
        }
        
        //attach listeners
        addComponentListener(handler);
        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
        
    }
    public void activate(){
        activated = true;
    }
    public void deactivate(){
        activated = false;
    }
    public void setDefaultFont(Font ft){
        activeFont = ft;
        metrics = new Metrics(activeFont);
        layout.revalidate();
    }
    public void setDefaultColor(Color cc){
        baseColor = cc.getRGB();
        model.setDefaultColor(baseColor);//leave it or update existing?
    }
    public void setGap(int pixels){
        layout.setLineGap(pixels);
    }
    public int getGap(){
        return layout.gap;
    }
    //Clears existing and loads model
    public void setText(File file){
        //erase existing, if any
        model.clear();
        layout.initModel(Metrics.harvest(file));
    }
    public void setText(String text){
        model.clear();
        layout.initModel(Metrics.harvest(text));
    }
    public void clearText(){
        model.clear();
        if(isEditable){
            caret.line = 0;
            caret.column = 0;
        }
        layout.revalidate();
    }
    public void setEditable(boolean isEditing){//too many config places
        isEditable = isEditing;
        if(isEditable){
            setEditor(null);//sets default editor
        } else {
            if(editor != null){
                removeKeyListener(handler);
                editor.caret = null;
                editor.model = null;
                editor.highlight = null;
                editor = null;
                caret.setEnabled(false);
            }
        }
    }
    public void setEditor(Editor ed){
        isEditable = true;
        editor = ed != null ? ed : new BasicEditor();
        editor.caret = caret;
        editor.setModel(model);
        editor.highlight = highlight;
        addKeyListener(handler);
        caret.setEnabled(true);
        initialized = false;
    }
    public void setWordWrap(boolean useWordWrap){
        layout.wordwrap = useWordWrap;
        layout.revalidate();
    }
    public void setHorizPolicy(int hp){
        scroll.setHorizScrollPolicy(hp);
    }
    public void setVertPolicy(int vp){
        scroll.setVerticalScrollPolicy(vp);
    }
    //Document Interface
    /**
     * These block to preserve consistency of model.
     * Do not call from event thread.
     * Text methods emulate an infinite length and width document,
     * so if insert(line 88) is called and doc is only 4 lines long,
     * the model will pad lines upto line 88!
     */
    //Starts a new line at end of document
    public void attachText(int col, String text){
        modelLock.lock();
        model.appendDocument(col, text);
        layout.revalidate();
        modelLock.unlock();
    }
    //Simple add to end of line
    public void addText(String text){
        modelLock.lock();
        model.appendLine(caret.line, text);
        layout.revalidate();
        modelLock.unlock();
    }
    public void addText(String text, int line){
        modelLock.lock();
        model.appendLine(line, baseColor, text);
        layout.revalidate();
        modelLock.unlock();
    }
    public void addText(String text, int line, Color color){
        modelLock.lock();
        model.appendLine(line, color.getRGB(), text);
        layout.revalidate();
        modelLock.unlock();
    }
    
    public void insertText(String text, int line, int col){//could just schedule a message...
        modelLock.lock();
        model.insert(line, col, baseColor, text);
        layout.revalidate();
        modelLock.unlock();
    }
    public void insertText(String sdata, int line, int col, Color color){
        modelLock.lock();
        model.insert(line, col, color.getRGB(), sdata);
        layout.revalidate();
        modelLock.unlock();
    }
    public void overwriteText(String text){
        
    }
    //Text activity at caret
    public void insertTextAtCaret(String text){
        modelLock.lock();
        model.insert(caret.line, caret.column, text);
        layout.revalidate();
        modelLock.unlock();
    }
    //New Delete
    public void deleteText(int line, int col){//single position
        modelLock.lock();
        model.delete(line, col, line, col);
        layout.revalidate();
        modelLock.unlock();
    }
    public void deleteText(int line, int startCol, int endCol){
        modelLock.lock();
        model.delete(line, startCol, line, endCol);
        layout.revalidate();
        modelLock.unlock();
    }
    public void deleteText(int startLine, int startCol, int endLine, int endCol){//inclusive
        modelLock.lock();
        model.delete(startLine, startCol, endLine, endCol);
        layout.revalidate();
        modelLock.unlock();
    }
    public void deleteLine(int line){
        deleteLines(line, line);
    }
    public void deleteLines(int start, int end){
        modelLock.lock();
        model.deleteLines(start, end);
        layout.revalidate();
        modelLock.unlock();
    }
    //End Delete
    //Get
    public char[] getText(int line, int start, int end){
        if(line < 0 || line >= model.getLines()) return null;
        return model.getChars(line, start, end);
    }
    public char[] getText(int startLine, int startCol, int endLine, int endCol){
        return model.get(startLine, startCol, endLine, endCol);
    }
    public char getChar(int line, int col){
        return model.getChar(line, col);
    }
    public char getLastChar(){
        return model.getChar(caret.line, caret.column);
    }
    //Adds multiline data to end of document
    public void addLine(String text){
        modelLock.lock();
        model.addLine(text);
        layout.revalidate();
        modelLock.unlock();
    }
    //Inserts multiline data anywhere
    public void insertLine(int line, String text){
        modelLock.lock();
        model.insertLine(line, text);
        layout.revalidate();
        modelLock.unlock();
    }
    //Returns null if model empty.
    public char[] getSaveData(){
        if(model.nlines > 0){
            modelLock.lock();
            char[] data = model.getLinearModel();
            layout.revalidate();
            modelLock.unlock();
            return data;
        }
        return null;
    }
    
    //Painting Thread
    //Important: dont manually set camWidth or docWidth. Use the update methods.
    @Override
    protected void paintComponent(Graphics g) {
        //super.paintComponent(g);//clears screen
        
        if(activated){
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //System.out.println("Draw");
            //Update scroller values
            //scroll.updateScroller(getWidth(), getHeight());//model measured in init
            
            
            if(!initialized){
                
                if(isEditable){//grab focus and enable Tab key
                    //Mandatory for keylistener to work
                    requestFocusInWindow();
                    //Allows keylistener to receive Tab key
                    setFocusTraversalKeysEnabled(false);
                }
                initialized = true;
                if(layout.wordwrap){//scroller up, hit the word wrap
                    layout.revalidate();
                }
            }
            //If system is in use, bail: what if painting and wilbur starts to modify? bufferedImage?
            if(!modelLock.isLocked){//Non-blocking
                super.paintComponent(g);
                //draw model
                drawModel(g2);
                //draw caret if visible
                if(caret.caretVisible){
                    g2.setColor(caret.color);
                    g2.fill(caret.getCaret());
                }
                //draw highlight if on
                if(highlight.visible){
                    g2.setColor(highlight.color);
                    g2.fill(highlight.getHighlight());
                }
                //now draw scroller on top, if present
                scroll.drawScroller(g2);
            }
            
        } else {
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    /**
     * Draw the visible set of baselines in camSpace.
     * Cell-based draw algo.
     * Cant do horizontal same as vertical: letters aren't same width!
     * Draw set only applies to lines. On each line, the entire thing is drawn.
     * @param g 
     */
    private void drawModel(Graphics2D g){
        g.setFont(activeFont);
        int lineheight = metrics.textheight + layout.gap;
        //calc last line of draw set
        int lineEnd = Math.min((scroll.camy + scroll.getAvailableHeight()) / lineheight, model.nlines);
        int vy, vx;//need some kind of border or padding
        FontMetrics fm = g.getFontMetrics(activeFont);
        int lineStart = scroll.camy / lineheight;
        model.setArrow(lineStart);
        //int maxWidth = 0;
        
        for (int line = lineStart; line < lineEnd; line++) {
            vy = line * lineheight;
            vx = 0;//horiz position
            
            int cc;
            int width = model.arrowWidth();
            int start = 0, end = 0;
            String stemp = null;
            while(end < width){
                cc = model.arrowColor(start);
                end = model.getColorChangeAtArrow(start);
                stemp = model.charsAtArrow(start, end);
                g.setColor(new Color(cc));
                g.drawString(stemp, vx - scroll.camx, vy + layout.gap + metrics.ascent - scroll.camy);
                vx += fm.stringWidth(stemp);
                start = end;
            }
            //if(vx > maxWidth) maxWidth = vx;//track the widest line
            model.next();
        }
        //docHeight = model.getLines() * (metrics.textheight + gap);
        //docWidth = maxWidth;//only way to get this
    }
    private void testZoom(Graphics2D g){
        TextLayout ty = new TextLayout("Text", new Font("Helvetica", 1, 96), new FontRenderContext(null, false, false));
        AffineTransform aft = new AffineTransform();
        aft.translate(0, (float)ty.getBounds().getHeight());
        Shape outline = ty.getOutline(aft);
        PathIterator p = outline.getPathIterator(null);
        
        AffineTransform saveXform = g.getTransform();
        g.transform(aft);
        //reset old
        g.setTransform(saveXform);
    }

    //Protected Mutable State
    int bcapacity = 100;//guess
    Message[] buffer = new Message[bcapacity];
    Semaphore emptySlots = new Semaphore(bcapacity);//producer's constraint
    Semaphore fullSlots = new Semaphore(0);//consumer's constraint
    Lock mutex = new Lock();
    int bin, bout;

    private class Processor implements Runnable {
        Thread worker;
        boolean alive;
        ExecutorService exe = Executors.newFixedThreadPool(10);
        
        public Processor(){}
        public void start(){
            alive = true;
            worker = new Thread(this, "Wilbur");
            worker.setDaemon(true);
            worker.start();
        }
        public void stop(){
            alive = false;
        }
        
        public void send(Message msg){
            if(msg == null) return;
            exe.submit(new DropTask(msg));
            //CompletableFuture<Void> fut = CompletableFuture.supplyAsync(null, exe);
            //CompletableFuture<Void> fut2 = CompletableFuture.runAsync(new DropTask(msg));
            //powerful methods could be applied to future
        }
        
        //<<<<<<<<<<<<<<<<<<THIS IS THE RUNTIME LOGIC>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        
        @Override
        public void run() {
            while (alive) {
                fullSlots.semWait();//wait if nothing available
                mutex.lock();
                Message got = buffer[bout];//snag msg
                bout = (bout + 1) % bcapacity;
                mutex.unlock();//now get the hell outta here
                emptySlots.semSignal();//signal space available
                //now process message
                if(got instanceof KeyMessage){
                    //System.out.println("KeyPress");
                    KeyMessage km = (KeyMessage)got;
                    modelLock.lock();
                    editor.handleKeyPress(km.pressed, km.isShift);
                    layout.revalidate();
                    modelLock.unlock();
                    if(isEditable && !scroll.isCellVisible(caret.line, caret.column)){
                        scroll.makeVisible(caret.line, caret.column);
                    }
                } else if(got instanceof MousePressedMessage){
                    //System.out.println("MousePressed");
                    MousePressedMessage mp = (MousePressedMessage)got;
                    if(scroll.mousePressed(mp.mx, mp.my)){
                        //doesnt affect scrollSys, send elsewhere
                        if(Metrics.isLeftMouseButton(mp.modifiers)){
                            highlight.mousePress(mp.mx, mp.my);
                        }
                    }
                } else if(got instanceof MouseReleasedMessage){
                    //System.out.println("MouseRelease");
                    //MouseReleasedMessage mr = (MouseReleasedMessage)got;
                    if(scroll.mouseReleased()){
                        highlight.mouseRelease();
                    }
                } else if(got instanceof MouseClickedMessage){
                    //scroll.test();
                    MouseClickedMessage mc = (MouseClickedMessage)got;
                    if(scroll.mouseClicked(mc.mx, mc.my)){
                        //System.out.println("camx: " + scroll.camx + " camy: " + scroll.camy + " docWidth: " + scroll.docWidth + " docHgt: " + scroll.docHeight);
                        if(Metrics.isLeftMouseButton(mc.modifiers)){
                            caret.moveCaretByMouse(mc.mx, mc.my);
                            highlight.mouseClicked(mc.clickCount, mc.mx, mc.my);
                        } else if(Metrics.isRightMouseButton(mc.modifiers)){
                            rightClick.show(Textbox.this, mc.mx, mc.my);
                        }
                    }
                } else if(got instanceof MouseDraggedMessage){
                    //System.out.println("MouseDrag");
                    MouseDraggedMessage md = (MouseDraggedMessage)got;
                    if(scroll.mouseDragged(md.currentX, md.currentY)){
                        highlight.mouseDragged(md.currentX, md.currentY);
                    }
                } else if(got instanceof MouseWheelMessage){
                    //System.out.println("MouseWheel");
                    MouseWheelMessage mw = (MouseWheelMessage)got;
                    scroll.mouseWheel(mw.ticks);
                } else if(got instanceof ResizeMessage){
                    ResizeMessage res = (ResizeMessage)got;
                    
                    scroll.resize(res.width, res.height);
                    //no action needed other than repaint below.
                    //System.out.println("Resize wid: " + res.width + ", hgt: " + res.height);
                } else if(got instanceof EditorAction){
                    EditorAction ea = (EditorAction)got;
                    modelLock.lock();
                    switch(ea.action){
                        case CUT: highlight.cut(); break;
                        case COPY: highlight.copy(); break;
                        case PASTE: highlight.paste(); break;
                    }
                    layout.revalidate();
                    modelLock.unlock();
                }
                //perform a system-wide repaint
                //System.out.println("System Repaint");
                Textbox.this.repaint();//ONLY ALLOWED REPAINT
            }
        }
    }
    private class DropTask implements Runnable {
        Message message;
        public DropTask(Message msg){ message = msg; }
        
        @Override
        public void run() {
            emptySlots.semWait();
            mutex.lock();
            buffer[bin] = message;
            bin = (bin + 1) % bcapacity;
            mutex.unlock();
            fullSlots.semSignal();
        }
    }

    private class Semaphore {
        int slips;
        public Semaphore(int permits) {
            slips = permits;
        }
        //when driven below 0, blocks all calling threads.
        public void semWait() {
            synchronized (this) {
                slips--;
                while (slips < 0) {//while handles spurious wake-ups
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        public void semSignal() {
            synchronized (this) {
                slips++;
                this.notify();
            }
        }
    }
    private class Lock {
        private boolean isLocked = false;
        public Lock() { }
        //only the first thread can make it through here.
        //Not a SpinLock: other threads put to sleep
        public void lock() {//the only busy-wait is to call this method to SEE value of lock
            //ensures only 1 thread at a time checks value of lock
            synchronized (this) {//is equivalent to while(testAndSet(gaurd));
                while (isLocked) {
                    try { this.wait();
                    } catch (InterruptedException e) {
                    }
                }
                isLocked = true;
            }
        }
        //blocking
        public void unlock() {
            synchronized (this) {
                isLocked = false;
                this.notify();
            }
        }
        //Non-blocking
        public boolean isLocked() {
            return isLocked;
        }
    }

    //Event Thread Entry Points
    private class Handler extends ComponentAdapter implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {
        
        @Override
        public void keyTyped(KeyEvent ke) { }
        @Override
        public void keyPressed(KeyEvent ke) {
            core.send(new KeyMessage(ke.getKeyCode(), ke.isShiftDown()));
        }
        @Override
        public void keyReleased(KeyEvent ke) { }
        @Override
        public void mouseClicked(MouseEvent me) {
            core.send(new MouseClickedMessage(me.getClickCount(), me.getX(), me.getY(), me.getModifiers()));
        }
        @Override
        public void mousePressed(MouseEvent me) {
            core.send(new MousePressedMessage(me.getX(), me.getY(), me.getModifiers()));
        }
        @Override
        public void mouseReleased(MouseEvent me) {
            core.send(new MouseReleasedMessage());
        }
        @Override
        public void mouseEntered(MouseEvent me) { }
        @Override
        public void mouseExited(MouseEvent me) { }
        @Override
        public void mouseDragged(MouseEvent me) {
            core.send(new MouseDraggedMessage(me.getX(), me.getY()));
        }
        @Override
        public void mouseMoved(MouseEvent me) { }
        @Override
        public void mouseWheelMoved(MouseWheelEvent mwe) {
            core.send(new MouseWheelMessage(mwe.getWheelRotation()));
        }
        @Override
        public void componentResized(ComponentEvent ce) {
            Component com = ce.getComponent();
            core.send(new ResizeMessage(com.getWidth(), com.getHeight()));
        }
        @Override//only for right click menu
        public void actionPerformed(ActionEvent ae) {
            String command = ae.getActionCommand();
            int type = 0;
            switch(command){
                case "cut": type = CUT; break;
                case "copy": type = COPY; break;
                case "paste": type = PASTE; break;
            }
            core.send(new EditorAction(type));
        }
    }

    private class Message { }

    private class KeyMessage extends Message {
        boolean isShift;
        int pressed;
        public KeyMessage(int keyCode, boolean shiftDown) {
            pressed = keyCode;
            isShift = shiftDown;
        }
    }
    private class MouseWheelMessage extends Message {
        int ticks;
        public MouseWheelMessage(int ticks) {
            this.ticks = ticks;
        }
    }
    private class MousePressedMessage extends Message {
        int mx;
        int my;
        int modifiers;
        public MousePressedMessage(int mx, int my, int mod) {
            this.mx = mx;
            this.my = my;
            this.modifiers = mod;
        }
    }
    private class MouseClickedMessage extends Message {
        int clickCount;
        int mx;
        int my;
        int modifiers;
        public MouseClickedMessage(int clickCount, int mx, int my, int mod) {
            this.clickCount = clickCount;
            this.mx = mx;
            this.my = my;
            this.modifiers = mod;
        }
    }
    private class MouseReleasedMessage extends Message { }
    private class MouseDraggedMessage extends Message {
        int currentX, currentY;
        public MouseDraggedMessage(int currentX, int currentY) {
            this.currentX = currentX;
            this.currentY = currentY;
        }
    }
    private class ResizeMessage extends Message {
        int width;
        int height;
        public ResizeMessage(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    private static final int CUT = 1;
    private static final int COPY = 2;
    private static final int PASTE = 3;
    private class EditorAction extends Message {
        int action;
        public EditorAction(int action) {
            this.action = action;
        }
    }
    private JPopupMenu createMenu(){
        JPopupMenu jpm = new JPopupMenu("RightClick");
        
        JMenuItem create = new JMenuItem("Copy");
        create.setActionCommand("copy");
        create.addActionListener(handler);
        
        JMenuItem details = new JMenuItem("Cut");
        details.setActionCommand("cut");
        details.addActionListener(handler);
        
        JMenuItem remove = new JMenuItem("Paste");
        remove.setActionCommand("paste");
        remove.addActionListener(handler);
        
        JMenuItem select = new JMenuItem("Select All");
        select.setActionCommand("select");
        select.addActionListener(handler);
        
        jpm.add(create);
        jpm.add(details);
        jpm.add(remove);
        jpm.addSeparator();
        jpm.add(select);
        
        return jpm;
    }
    public static void main(String[] args) {
        Textbox tex = new Textbox();
        tex.setWordWrap(false);
        tex.setEditable(true);
        tex.setHorizPolicy(ScrollSys.WHEN_NEEDED);
        
        tex.activate();
    }
}
