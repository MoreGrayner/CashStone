package io.github.moregrayner.miniame.cashStone.manager

import io.github.moregrayner.miniame.cashStone.CashStone

class ConfigManager(plugin: CashStone) {

    init {
        plugin.saveConfig()
    }
}