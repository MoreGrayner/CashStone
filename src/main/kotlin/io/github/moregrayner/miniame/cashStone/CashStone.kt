package io.github.moregrayner.miniame.cashStone


import io.github.moregrayner.miniame.cashStone.commands.*
import io.github.moregrayner.miniame.cashStone.listener.*
import io.github.moregrayner.miniame.cashStone.manager.*
import io.github.moregrayner.miniame.cashStone.scedulers.AttackRightScheduler
import io.github.moregrayner.miniame.cashStone.scedulers.GameStateScheduler
import io.github.moregrayner.miniame.cashStone.scedulers.GameTickScheduler
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.util.logging.Level

class CashStone : JavaPlugin() {
    internal var gameManager: GameManager? = null
    internal var teamManager: TeamManager? = null
    internal var shopManager: ShopManager? = null
    var cashManager1: CashManager? = null
        private set
    private var uIManager: UIManager? = null
    internal var miningManager: MiningManager? = null
    internal var battleManager: BattleManager? = null
    private var configManager: ConfigManager? = null
    private var coremanager: CoreManager? = null

    // 데이터 파일들
    private var cashDataFile: File? = null
    var cashData: FileConfiguration? = null
        private set

    override fun onEnable() {
        instance = this


        saveDefaultConfig()
        createCustomConfigs()


        initializeManagers()


        registerCommands()


        registerListeners()

        startSchedulers()

        logger.info("석쩐!")
    }

    override fun onDisable() {
        // 게임 종료 처리
        gameManager?.forceEndGame()


        // 데이터 저장
        saveCashData()

        logger.info("석쩐...")
    }

    private fun initializeManagers() {
        configManager = ConfigManager(this)
        gameManager = GameManager(this)
        teamManager = TeamManager(this)
        cashManager1 = CashManager(this)
        shopManager = ShopManager(this)
        uIManager = UIManager()
        miningManager = MiningManager(this)
        battleManager = BattleManager(this)
        coremanager = CoreManager(this)
    }

    private fun registerCommands() {
        getCommand("assign")!!.setExecutor(AssignCommand(this))
        getCommand("stonewar")!!.setExecutor(StoneWarCommand(this))
        getCommand("shop")!!.setExecutor(ShopCommand(this))
        getCommand("cash")!!.setExecutor(CashCommand(this))
        getCommand("team")!!.setExecutor(TeamCommand(this))
    }

    private fun registerListeners() {
        Bukkit.getPluginManager().registerEvents(PlayerListener(this), this)
        Bukkit.getPluginManager().registerEvents(InventoryListener(this), this)
        Bukkit.getPluginManager().registerEvents(MiningListener(this), this)
        Bukkit.getPluginManager().registerEvents(CraftingListener(), this)
        coremanager?.let { CoreListener(it) }?.let { Bukkit.getPluginManager().registerEvents(it, this) }
    }

    private fun startSchedulers() {
        GameTickScheduler(this).runTaskTimer(this, 0L, 20L) // 10배 느림


        // 공격권 만료 체크 스케줄러
        AttackRightScheduler(this).runTaskTimer(this, 0L, 20L) // 1초마다


        // 게임 상태 업데이트 스케줄러
        GameStateScheduler(this).runTaskTimer(this, 0L, 20L)

        battleManager?.startMarkerGlowTask()
    }

    private fun createCustomConfigs() {
        val file = File(dataFolder, "cash.yml")
        if (!file.exists()) {
            saveResource("cash.yml", false)
        }
        cashDataFile = file
        cashData = YamlConfiguration.loadConfiguration(file)
    }


    fun saveCashData() {
        try {
            cashData!!.save(cashDataFile!!)
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "캐시 데이터를 저장할 수 없습니다!", e)
        }
    }

    fun reloadCashData() {
        cashData = YamlConfiguration.loadConfiguration(cashDataFile!!)
    }

    fun getGameManager(): GameManager? {
        return gameManager
    }

    fun getTeamManager(): TeamManager? {
        return teamManager
    }

    fun getShopManager(): ShopManager? {
        return shopManager
    }

    fun getMiningManager(): MiningManager? {
        return miningManager
    }

    fun getBattleManager(): BattleManager? {
        return battleManager
    }

    fun getConfigManager(): ConfigManager? {
        return configManager
    }

    fun getCashManager(): CashManager?{
        return cashManager1
    }

    fun getCoreManager(): CoreManager?{
        return coremanager
    }

    companion object {
        // Getters
        var instance: CashStone? = null
            private set
    }
}
