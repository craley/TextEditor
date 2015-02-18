/**
 *
 * 
 *
 */

package realdeal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

/**
 *
 * @author chris
 */
public class TextEditor extends JFrame {
    JTabbedPane content;
    FileWindow filesys;
    List<Context> openList = new LinkedList<>();

    public TextEditor(){
        initialize();
    }
    private void initialize(){
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        filesys = new FileWindow(this);//need listener instead
        
        //content = new JTabbedPane();
        //content.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        JPanel jp = new JPanel(new BorderLayout());
        
        //box = new Textbox(new File("testfile"));//new File("testfile")
        box = new Textbox(new JavaEditor(), new File("testfile"));
        //box.insertText("Plug", 3, 5);
        box.setEditable(true);
//        //box.setHorizPolicy(ScrollSys.NEVER);
        jp.add(box);
        JPanel bottom = new JPanel();
        JButton submit = new JButton("Submit");
        JButton test = new JButton("Test");
        bottom.add(submit);
        bottom.add(test);
        jp.add(bottom, BorderLayout.SOUTH);
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filesys, jp);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(0.5f);//can be double or int
        split.setDividerSize(15);
        filesys.setMinimumSize(new Dimension(100,400));
        jp.setMinimumSize(new Dimension(300,400));
        
        add(split);
        //add(box, BorderLayout.CENTER);
    }
    
    Textbox box;
    public void openFile(File file){
        if(alreadyOpen(file)){
            System.out.println("already open");
            return;
        }
        
    }
    private void closeFile(){
        
    }
    
    public void addTab(){
        box.activate();
    }
    private void interpretFile(String fileName){
        
    }
    private void layoutTextbox(TextboxOld box, File file){
        
    }
    private boolean alreadyOpen(File test){
        int cs = openList.size();
        for (int c = 0; c < cs; c++) {
            if(test.equals((openList.get(c)).file))
                return true;
        }
        return false;
    }
    
    class Context {
        File file;
        public Context(){
            
        }
        @Override
        public boolean equals(Object obj){
            if(obj != null && obj instanceof Context){
                Context other = (Context)obj;
                return file.equals(other.file);
            }
            return false;
        }
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            TextEditor te = new TextEditor();
            te.setVisible(true);
            te.addTab();
        });
    }
}
