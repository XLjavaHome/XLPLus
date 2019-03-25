package com.xiuyukeji.plugin.translation;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.popup.PopupFactoryImpl;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created with 徐立.
 *
 * @author 徐立
 * @version 1.0 2019-03-25 9:33
 * To change this template use File | Settings | File Templates.
 * @date 2019-03-25
 * @time 9:33
 */
public class AppendStringAction extends EditorAction {
    private long mLatestClickTime;
    
    protected AppendStringAction() {
        super(new AppendStringAction.Handler());
    }
    
    public static class Handler extends EditorWriteActionHandler {
        public Handler() {
        }
        
        @Override
        public void executeWriteAction(final Editor editor, DataContext dataContext) {
            if (!editor.getSelectionModel().hasSelection(true)) {
                if (Registry.is("editor.skip.copy.and.cut.for.empty.selection")) {
                    return;
                }
                //若没有选中内容，则默认选中光标停留的那一行
                editor.getCaretModel().runForEachCaret(new CaretAction() {
                    @Override
                    public void perform(Caret var1x) {
                        editor.getSelectionModel().selectLineAtCaret();
                    }
                });
            }
            SelectionModel sm = editor.getSelectionModel();
            //获取选中的内容
            String txt = sm.getSelectedText();
            String[] contextArray = StringUtils.split(txt, "\n");
            Set<String> resultSet = new LinkedHashSet<>(contextArray.length);
            //结果
            String resultStr;
            if (contextArray.length > 1 && contextArray[0].matches("\\d{3,}")) {
                //BUG摘要
                resultStr = handBugAbstract(contextArray, resultSet);
            } else {
                //加序号
                resultStr = handOrdNumer(editor, contextArray, resultSet);
            }
            //覆盖插入
            if (StringUtil.isEmpty(resultStr)) {
                showPopupBalloon(editor, "没有找到字符");
            }
            //将结果替换所选内容
            EditorModificationUtil.insertStringAtCaret(editor, resultStr, true, false);
        }
        
        /**
         * 显示汽包
         *
         * @param mEditor
         * @param result
         */
        private void showPopupBalloon(final Editor mEditor, final String result) {
            ApplicationManager.getApplication().invokeLater(() -> {
                mEditor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null);
                JBPopupFactory factory = JBPopupFactory.getInstance();
                factory.createHtmlTextBalloonBuilder(result, null, new JBColor(Gray._242, Gray._0), null).createBalloon()
                       .show(factory.guessBestPopupLocation(mEditor), Balloon.Position.below);
            });
        }
    }
    
    /**
     * 处理BUG摘要
     *
     * @param strings
     * @param result
     * @return
     */
    @NotNull
    private static String handBugAbstract(String[] strings, Set<String> result) {
        result.add(strings[0]);
        result.add(strings[strings.length - 1]);
        return StringUtil.join(result, ":");
    }
    
    /**
     * 给每一行加上序号<br/>
     * 1.如果上一行有序号序号累加<br/>
     * 2.如果上一行没有序号则从1开始<br/>
     * 去除两端空格，行尾加句号（有的话不用加）
     *
     * @param editor
     * @param strings
     * @param result
     * @return
     */
    @NotNull
    private static String handOrdNumer(Editor editor, String[] strings, Set<String> result) {
        //序号
        int num = initNum(editor);
        for (int i = 0; i < strings.length; i++) {
            String dataString = strings[i];
            String dataStrings = dataString.trim();
            if (StringUtil.isEmpty(dataStrings)) {
                continue;
            }
            StringBuilder string = new StringBuilder(dataStrings);
            //结尾加上句号
            if (string.lastIndexOf("。") < string.length() - 1) {
                string.append("。");
            }
            //选中有序号
            if (string.toString().matches("(\\d+\\.).*")) {
                num = Integer.parseInt(string.substring(0, string.indexOf(".")));
                result.add(string.toString());
            } else {
                result.add(string.insert(0, ++num + ".").toString());
            }
        }
        //Messages.showInfoMessage(String.format("选中了%s行", strings.length), "Count Result");
        return StringUtil.join(result, "\n");
    }
    
    /**
     * 初始化序号：选中的上一行的序号
     *
     * @param editor
     * @return
     */
    private static int initNum(Editor editor) {
        int selectionStart = editor.getSelectionModel().getSelectionStart();
        String beforeText = editor.getDocument().getText(new TextRange(0, selectionStart));
        if (StringUtils.isNotEmpty(beforeText)) {
            String[] beforeTexts = beforeText.split("\n");
            //最后一行的开始字符
            String beforeText1 = beforeTexts[beforeTexts.length - 1];
            if (beforeText1.matches("(\\d+\\.).*")) {
                return Integer.parseInt(beforeText1.substring(0, beforeText1.indexOf(".")));
            }
        }
        return 0;
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