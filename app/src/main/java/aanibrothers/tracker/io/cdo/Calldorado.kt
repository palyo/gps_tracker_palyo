package aanibrothers.tracker.io.cdo

import aanibrothers.tracker.io.*
import aanibrothers.tracker.io.module.isPremium
import android.content.*
import com.calldorado.*
import com.calldorado.Calldorado.SettingsToggle
import com.calldorado.Calldorado.acceptConditions

fun Context.initCalldorado() {
    Calldorado.start(this)
    val colorMap = HashMap<Calldorado.ColorElement?, Int?>()
    colorMap[Calldorado.ColorElement.AftercallBgColor] = getColor(coder.apps.space.library.R.color.colorPrimary)
    colorMap[Calldorado.ColorElement.AftercallStatusBarColor] = getColor(coder.apps.space.library.R.color.colorBlack)
    colorMap[Calldorado.ColorElement.AftercallAdSeparatorColor] = getColor(coder.apps.space.library.R.color.colorIcon)
    colorMap[Calldorado.ColorElement.CardBgColor] = getColor(R.color.colorCardBackground)
    colorMap[Calldorado.ColorElement.CardTextColor] = getColor(coder.apps.space.library.R.color.colorText)
    colorMap[Calldorado.ColorElement.NavigationColor] = getColor(coder.apps.space.library.R.color.colorAccent)
    colorMap[Calldorado.ColorElement.TabIconButtonTextColor] = getColor(coder.apps.space.library.R.color.colorAccentTool)
    colorMap[Calldorado.ColorElement.SelectedTabIconColor] = getColor(coder.apps.space.library.R.color.colorAccentTool)
    colorMap[Calldorado.ColorElement.MainTextColor] = getColor(coder.apps.space.library.R.color.colorText)
    colorMap[Calldorado.ColorElement.DarkAccentColor] = getColor(coder.apps.space.library.R.color.colorAccent)
    Calldorado.setCustomColors(this, colorMap)
}

fun Context.eulaAccepted() {
    val conditionsMap: HashMap<Calldorado.Condition, Boolean> = HashMap()
    conditionsMap[Calldorado.Condition.EULA] = true
    conditionsMap[Calldorado.Condition.PRIVACY_POLICY] = true
    Calldorado.acceptConditions(this, conditionsMap)
}

fun Context.setCdoEnable(){
    val premium = !isPremium
    val conditionsMap: HashMap<Calldorado.Condition, Boolean> = HashMap()
    conditionsMap[Calldorado.Condition.EULA] = premium
    conditionsMap[Calldorado.Condition.PRIVACY_POLICY] = premium
    acceptConditions(this, conditionsMap)

    val settingsMap = HashMap<SettingsToggle, Boolean>()
    settingsMap[SettingsToggle.REAL_TIME_CALLER_ID] = premium
    settingsMap[SettingsToggle.MISSED_CALL] = premium
    settingsMap[SettingsToggle.COMPLETED_CALL] = premium
    settingsMap[SettingsToggle.NO_ANSWER_CALL] = premium
    settingsMap[SettingsToggle.UNKNOWN_CALL] = premium
    Calldorado.setSettings(this, settingsMap)
}