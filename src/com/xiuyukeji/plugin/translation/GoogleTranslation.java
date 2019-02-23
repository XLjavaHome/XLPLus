package com.xiuyukeji.plugin.translation;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.TextRange;
import com.xiuyukeji.plugin.translation.translator.impl.GoogleTranslator;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 谷歌翻译插件
 *
 * @author Created by jz on 2017/10/24 14:44
 */
public class GoogleTranslation extends AnAction {
    private long mLatestClickTime;
    private final GoogleTranslator mTranslator = new GoogleTranslator();
    
    public GoogleTranslation() {
        super(IconLoader.getIcon("/icons/translate.png"));
    }
    
    @Override
    public void actionPerformed(AnActionEvent e) {
        if (!isFastClick()) {
            try {
                getTranslation(e);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
    
    /**
     * 将内容翻译
     *
     * @param event
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void getTranslation(AnActionEvent event) throws ExecutionException, InterruptedException {
        //1.获取要翻译的文本
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        String queryText = getText(editor);
        //2.获取翻译后的内容
        String result = getTranslatedContent(editor, queryText);
        //3.复制进剪切板
        copyToClipboard(result);
    }
    
    /**
     * 获取翻译后的内容
     *
     * @param editor
     * @param queryText
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private String getTranslatedContent(Editor editor, String queryText) throws InterruptedException, ExecutionException {
        FutureTask<String> task = new FutureTask(new RequestRunnable(mTranslator, editor, queryText));
        new Thread(task).start();
        return task.get();
    }
    
    /**
     * 获取用户选择的内容或者当前行
     *
     * @param editor
     * @return
     */
    @NotNull
    private String getText(Editor editor) {
        SelectionModel model = editor.getSelectionModel();
        String selectedText = model.getSelectedText();
        if (TextUtils.isEmpty(selectedText)) {
            selectedText = getCurrentLine(editor);
        }
        return strip(addBlanks(selectedText));
    }
    
    /**
     * 复制至剪切板
     *
     * @param result
     */
    private void copyToClipboard(String result) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(result), null);
    }
    
    /**
     * 获取当前行
     *
     * @param editor
     * @return
     */
    private String getCurrentLine(Editor editor) {
        Document document = editor.getDocument();
        CaretModel caretModel = editor.getCaretModel();
        int caretOffset = caretModel.getOffset();
        int lineNum = document.getLineNumber(caretOffset);
        int lineStartOffset = document.getLineStartOffset(lineNum);
        int lineEndOffset = document.getLineEndOffset(lineNum);
        return document.getText(new TextRange(lineStartOffset, lineEndOffset));
    }
    
    private String addBlanks(String str) {
        String temp = str.replaceAll("_", " ");
        if (temp.equals(temp.toUpperCase())) {
            return temp;
        }
        return temp.replaceAll("([A-Z]+)", " $0");
    }
    
    private String strip(String str) {
        return str.replaceAll("/\\*+", "").replaceAll("\\*+/", "").replaceAll("\\*", "").replaceAll("//+", "")
                  .replaceAll("\r\n", " ").replaceAll("\\s+", " ");
    }
    
    /**
     * 确认你是不是快速点击
     *
     * @return true:不断点击，false：不是
     */
    private boolean isFastClick() {
        long time = System.currentTimeMillis();
        long timeD = time - mLatestClickTime;
        if (0 < timeD && timeD < (long) 1000) {
            return true;
        }
        mLatestClickTime = time;
        return false;
    }
}
