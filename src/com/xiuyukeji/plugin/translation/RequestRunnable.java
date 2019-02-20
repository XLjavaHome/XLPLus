package com.xiuyukeji.plugin.translation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.xiuyukeji.plugin.translation.translator.impl.GoogleTranslator;
import com.xiuyukeji.plugin.translation.translator.trans.Language;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * 请求
 *
 * @author Created by jz on 2017/10/24 14:45
 */
public class RequestRunnable implements Callable<String> {
    private GoogleTranslator mGoogleTranslator;
    private Editor mEditor;
    private String mQuery;
    
    RequestRunnable(GoogleTranslator translator, Editor editor, String query) {
        this.mEditor = editor;
        this.mQuery = query;
        this.mGoogleTranslator = translator;
    }
    
    @Override
    public String call() throws InterruptedException, ExecutionException {
        String text;
        if (isChinese(mQuery)) {
            text = mGoogleTranslator.translation(Language.ZH, Language.EN, mQuery);
        } else {
            text = mGoogleTranslator.translation(Language.EN, Language.ZH, mQuery);
        }
        String resultText = clearText(text);
        if (text == null) {
            showPopupBalloon("翻译出错！");
        } else {
            showPopupBalloon("翻译：" + resultText);
        }
        return resultText;
    }
    
    /**
     * 净化
     *
     * @param text
     * @return
     */
    private String clearText(String text) {
        if (text == null) {
            return null;
        }
        List<String> strings = StringUtil.split(text, " ");
        List<String> newString = new ArrayList<>(strings.size());
        if (strings.size() > 0) {
            for (int i = 0; i < strings.size(); i++) {
                String s = null;
                if (i == 0) {
                    s = strings.get(i).substring(0, 1).toLowerCase() + strings.get(i).substring(1);
                } else {
                    s = strings.get(i).substring(0, 1).toUpperCase() + strings.get(i).substring(1);
                }
                if (i == strings.size() - 1) {
                    s = s.replace("\r", "");
                    s = s.replace("\n", "");
                }
                newString.add(s);
            }
        }
        return StringUtil.join(newString, "");
    }
    
    private boolean isChinese(String strName) {
        char[] cs = strName.toCharArray();
        for (char c : cs) {
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
               || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
               || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
               || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
    
    private void showPopupBalloon(final String result) {
        ApplicationManager.getApplication().invokeLater(() -> {
            mEditor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null);//解决因为TranslationPlugin而导致的泡泡显示错位问题
            JBPopupFactory factory = JBPopupFactory.getInstance();
            factory.createHtmlTextBalloonBuilder(result, null, new JBColor(Gray._242, Gray._0), null).createBalloon()
                   .show(factory.guessBestPopupLocation(mEditor), Balloon.Position.below);
        });
    }
}
