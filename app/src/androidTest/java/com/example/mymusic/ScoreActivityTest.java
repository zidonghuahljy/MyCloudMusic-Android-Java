package com.example.mymusic;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ixuea.courses.mymusic.R;
import com.ixuea.courses.mymusic.activity.ScoreActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ScoreActivityTest {

    @Rule
    public ActivityScenarioRule<ScoreActivity> activityRule =
            new ActivityScenarioRule<>(ScoreActivity.class);

    @Test
    public void inputScore75_showsGradeB() {
        onView(withId(R.id.et_score)).perform(replaceText("75"));
        onView(withId(R.id.btn_classify)).perform(click());
        onView(withId(R.id.tv_result)).check(matches(withText("等级：B")));
    }

    @Test
    public void inputScore50_showsGradeC() {
        onView(withId(R.id.et_score)).perform(replaceText("50"));
        onView(withId(R.id.btn_classify)).perform(click());
        onView(withId(R.id.tv_result)).check(matches(withText("等级：C")));
    }

    // 故意不测 score >= 90，让 return "A" 保持红色（uncovered）
}
