/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.amlcurran.showcaseview;

import android.app.Activity;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Thomas
 */
public class ShowcaseManager {

    private Activity context;
    private Queue<Target> targets;
    private Queue<String> titles;
    private Queue<String> texts;

    public ShowcaseManager() {
        targets = new LinkedList<Target>();
        titles = new LinkedList<String>();
        texts = new LinkedList<String>();
    }

    public void showNext() {
        if (!targets.isEmpty()) {
            
            Target target = targets.poll();
            String title = titles.poll();
            String text = texts.poll();
            new ShowcaseView.Builder(context, this)
                    .setTarget(target)
                    .setContentTitle(title)
                    .setContentText(text)
                    .hideOnTouchOutside()
                    .build();
        }
    }

    public void addStub(Target target, int title, int text) {
        targets.offer(target);
        titles.offer(context.getString(title));
        texts.offer(context.getString(text));
    }

    public void setContext(Activity c) {
        this.context = c;
    }

}
