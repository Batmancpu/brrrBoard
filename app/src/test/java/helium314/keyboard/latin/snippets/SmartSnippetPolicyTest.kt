// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.snippets

import org.junit.Assert.*
import org.junit.Test

class SmartSnippetPolicyTest {
    private val pkg = "example.editor"
    private val rule = SnippetRule(1,"a".repeat(64),packages=setOf(pkg))
    private fun SmartSnippetSession.eligible(r: SnippetRule=rule, time:Long=0, boundary:SnippetBoundary=SnippetBoundary.EMPTY,day:Int=20261128,visible:Boolean=false) =
        eligible(r,pkg,time,day,7,12,boundary,visible)
    @Test fun fixedDeadlineAndReappearance() {
        val s=SmartSnippetSession();s.enter(pkg,100,listOf(rule))
        assertFalse(s.eligible(time=100,boundary=SnippetBoundary.NONE))
        assertTrue(s.eligible(time=1000))
        s.markShown(1)
        assertFalse(s.eligible(time=2000,boundary=SnippetBoundary.NONE))
        assertTrue(s.eligible(time=2500))
        assertTrue(s.eligible(time=3000,boundary=SnippetBoundary.NEW_LINE))
        s.enter(pkg,100000,listOf(rule))
        assertFalse(s.eligible(time=120100))
    }
    @Test fun hidePreservesSessionAndDailyUse() {
        val s=SmartSnippetSession();s.enter(pkg,0,listOf(rule));assertTrue(s.eligible())
        s.markShown(1);s.consume(1);s.hide(1000);s.enter(pkg,2000,listOf(rule))
        assertFalse(s.eligible(time=2000))
        s.enter("example.other",3000,emptyList());s.enter(pkg,4000,listOf(rule))
        assertTrue(s.eligible(time=4000))
        assertFalse(s.eligible(rule.copy(usedDay=20261128),4000))
    }
    @Test fun noRepeatAndExplicitKeyboardCadence() {
        val r=rule.copy(emptyAgain=false,newLine=false,cadence=SnippetCadence.KEYBOARD)
        val s=SmartSnippetSession();s.enter(pkg,0,listOf(r));assertTrue(s.eligible(r))
        s.markShown(1);assertTrue(s.eligible(r,100,visible=true))
        assertFalse(s.eligible(r,200));assertFalse(s.eligible(r,200,SnippetBoundary.NEW_LINE))
        s.hide(300);s.enter(pkg,400,listOf(r));assertTrue(s.eligible(r,400))
    }
    @Test fun selectedAppAndOvernightSchedule() {
        assertFalse(rule.matches("example.other"))
        assertTrue(rule.copy(allApps=true).matches("example.other"))
        val r=rule.copy(days=2,startHour=22,endHour=2)
        assertTrue(r.scheduled(2,23));assertTrue(r.scheduled(3,1))
        assertFalse(r.scheduled(3,23));assertFalse(r.scheduled(2,12))
        assertTrue(rule.valid());assertFalse(rule.copy(packages=emptySet()).valid())
    }
}
