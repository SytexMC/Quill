package me.levitate.quill.hook.hooks;

import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.hook.PluginHook;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook implements PluginHook {
    private static Economy economy;
    private boolean enabled = false;

    @Override
    public boolean init() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        economy = rsp.getProvider();
        enabled = true;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getPluginName() {
        return "Vault";
    }

    public boolean hasBalance(OfflinePlayer player, double amount) {
        return enabled && economy != null && economy.has(player, amount);
    }

    public double getBalance(OfflinePlayer player) {
        if (!enabled || economy == null) return 0.0;
        return economy.getBalance(player);
    }

    public boolean giveMoney(OfflinePlayer player, double amount) {
        if (!enabled || economy == null || amount < 0) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean takeMoney(OfflinePlayer player, double amount) {
        if (!enabled || economy == null || amount < 0) return false;
        if (!hasBalance(player, amount)) return false;

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean transferMoney(OfflinePlayer from, OfflinePlayer to, double amount) {
        if (!enabled || economy == null || amount < 0) return false;
        if (!hasBalance(from, amount)) return false;

        if (takeMoney(from, amount)) {
            if (giveMoney(to, amount)) {
                return true;
            } else {
                // Rollback if deposit fails
                giveMoney(from, amount);
            }
        }
        return false;
    }

    public void tryPurchase(Player player, double cost, Runnable successAction, Runnable failureAction) {
        if (!enabled || economy == null) {
            failureAction.run();
            return;
        }

        if (takeMoney(player, cost)) {
            successAction.run();
        } else {
            failureAction.run();
        }
    }

    public String formatMoney(double amount) {
        return enabled && economy != null ? economy.format(amount) : String.format("$%.2f", amount);
    }

    public String formatTransaction(double amount, boolean gained) {
        String formatted = formatMoney(amount);
        return gained ? "<green>+" + formatted : "<red>-" + formatted;
    }
}