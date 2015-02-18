/**
 *
 * 
 *
 */

package realdeal;

import java.awt.Color;
import java.util.Arrays;

/**
 *
 * @author chris
 */
public class JavaEditor extends BasicEditor {
    
    private static final String METHOD = "\n     \n}\n";
    private static final String[] ws = {"if","else", "public","private","protected","class","extends","package", "void", "int"};

    public JavaEditor(){
        
    }

    @Override
    public void setModel(Model m) {
        super.setModel(m);
        model.wordset.addAll(Arrays.asList(ws));
        model.setWordColor(Color.blue);
    }
    
    
    @Override
    public void symbol(char sym){
        if(sym == '('){
            //textbox.insertTextAtCursor("()");
            model.insert(caret.line, caret.column, "()");
            return;
        }
        //otherwise, let basic handle
        super.symbol(sym);
    }
//    @Override
//    public void insertNewline(){
////        char current = textbox.getLastChar();
////        switch(current){
////            case '{':
////                textbox.insertTextAtCursor(METHOD);
////                textbox.moveCaret(textbox.caretPos.line + 1, 5);
////                break;
////            default:
////                super.insertNewline();
////        }
//    }
}
