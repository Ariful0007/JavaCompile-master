package com.xiaoyv.java.ui.activity.main.editor;

import android.text.Html;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.xiaoyv.editor.common.Lexer;
import com.xiaoyv.java.JavaStudioSetting;
import com.xiaoyv.java.ui.activity.console.ConsoleActivity;
import com.xiaoyv.javaengine.JavaEngine;
import com.xiaoyv.javaengine.compile.listener.CompilerListener;

import java.io.File;
import java.util.HashMap;


/**
 * 编辑器
 *
 * @author 王怀玉
 * @since 2020/2/8
 */
public class EditorPresenter implements EditorContract.Presenter {
    @NonNull
    private final EditorContract.View view;

    public EditorPresenter(@NonNull EditorContract.View view) {
        this.view = view;
        view.setPresenter(this);
    }

    @Override
    public void start() {

    }

    /**
     * 导包
     */
    @Override
    public void findPackage(String selectedText, boolean isHide) {
        // 若选择的非单词，则跳过
        if (selectedText.trim().contains(" ")) {
            return;
        }

        HashMap<String, String> identifier = Lexer.getLanguage().getIdentifier();
        String className;
        if (identifier.containsKey(selectedText)) {
            className = identifier.get(selectedText);
            LogUtils.e("Found it:" + className);
        } else {
            if (!isHide)
                ToastUtils.showShort("JDK The class was not found in:" + selectedText);
            return;
        }

        String importText = "import " + className + ";";
        view.sendImportText(importText);
    }


    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void runFile(File javaFile) {
        view.showLog();
        view.showNormalInfo("Tips：The environment needs to be loaded for the first time\n\n>>> (Laughter)");

        File saveClassDir = new File(PathUtils.getExternalAppCachePath() + "/class");
        File saveDexFile = new File(PathUtils.getExternalAppCachePath() + "/dex/temp.dex");

        /*
         * 将 xxx.java 编译为 xxx.class 文件
         */
        JavaEngine.getClassCompiler().compile(javaFile, saveClassDir, new CompilerListener() {
            @Override
            public void onSuccess(String path) {
                ThreadUtils.runOnUiThread(() ->
                        view.showNormalInfo(String.format(">>> （1）\n>>> The output path：%s\n>>> The conversion begins",
                                path.replace(PathUtils.getExternalAppDataPath(), ""))));

                /*
                 * 将 xxx.class 转换为 xxx.dex 文件
                 */
                JavaEngine.getDexCompiler().compile(path, saveDexFile.getAbsolutePath(), new CompilerListener() {
                    @Override
                    public void onSuccess(String path) {
                        ThreadUtils.runOnUiThread(() -> {
                            view.showNormalInfo(String.format(">>> The conversion is complete\n>>> The output path：%s\nTips：Please close the log box to run the program",
                                    path.replace(PathUtils.getExternalAppDataPath(), "")));
                            view.showLogDismissListener(path);
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        ThreadUtils.runOnUiThread(() -> view.showErrorInfo(error.getMessage()));
                    }

                    @Override
                    public void onProgress(String task, int progress) {
                        view.showProgress(task.replace(PathUtils.getExternalAppDataPath(), ""), progress);
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                ThreadUtils.runOnUiThread(() -> view.showErrorInfo(error.getMessage()));
            }

            @Override
            public void onProgress(String task, int progress) {
                view.showProgress(task.replace(PathUtils.getExternalAppDataPath(), ""), progress);
            }
        });
    }

    @Override
    public void formatJavaCode(String sourceCode, boolean ignoreError) {
        ThreadUtils.executeByCached(new ThreadUtils.SimpleTask<Object>() {
            @Override
            public Object doInBackground() throws Throwable {
                JavaFormatterOptions javaFormatterOptions = JavaStudioSetting.getJavaFormatterOptions();
                Formatter formatter = new Formatter(javaFormatterOptions);
                return formatter.formatSource(sourceCode);
            }

            @Override
            public void onFail(Throwable t) {
                if (ignoreError) return;
                if (t instanceof FormatterException) {
                    FormatterException exception = (FormatterException) t;
                    view.formatCodeFail(exception);
                }
            }

            @Override
            public void onSuccess(Object result) {
                view.formatCodeSuccess(String.valueOf(result));
            }
        });
    }

}
