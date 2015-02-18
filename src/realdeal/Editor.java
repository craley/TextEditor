/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realdeal;

/**
 *
 * @author chris
 */
public abstract class Editor {
    CaretSys caret;
    Model model;
    HighlightSys highlight;
    int baseColor;
    
    public abstract void handleKeyPress(int pressed, boolean shift);
    public void setCaret(CaretSys cs){
        caret = cs;
    }
    public void setModel(Model m){
        model = m;
    }
    public void setDefaultColor(int c){
        baseColor = c;
    }
    public void setHighlight(HighlightSys hs){
        highlight = hs;
    }
    //dont need all these now!
//    public void arrows(int key);
//    public void tab();
//    public void backspace();
//    public void symbol(char sym);
//    public void toggleInsert();
//    public void insertNewline();
//    public void setTabWidth(int blocks);
}
