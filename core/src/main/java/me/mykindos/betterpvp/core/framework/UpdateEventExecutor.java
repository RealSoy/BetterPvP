package me.mykindos.betterpvp.core.framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

@Singleton
public class UpdateEventExecutor {

    private final Core core;

    public HashMap<Long, Long> lastRunTimers = new HashMap<>();

    @Inject
    public UpdateEventExecutor(Core core) {
        this.core = core;
    }

    public void initialize(){
        new BukkitRunnable() {
            @Override
            public void run() {
                executeUpdateEvents();
            }
        }.runTaskTimer(core, 0, 1);
    }

    private void executeUpdateEvents() {

        var updateTimers = new HashMap<Long, Long>();

        for (var plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin instanceof BPvPPlugin bPvPPlugin) {
                var listeners = bPvPPlugin.getListeners();
                for (var listener : listeners) {
                    var methods = listener.getClass().getDeclaredMethods();
                    for (var method : methods) {
                        if (method.isAnnotationPresent(UpdateEvent.class)) {
                            var updateEvent = method.getAnnotation(UpdateEvent.class);

                            if(lastRunTimers.containsKey(updateEvent.delay())) {
                                if (lastRunTimers.get(updateEvent.delay()) < System.currentTimeMillis()) {

                                    if (updateEvent.isAsync()) {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                executeMethod(method, listener);
                                            }
                                        }.runTaskAsynchronously(core);
                                    } else {
                                        executeMethod(method, listener);
                                    }


                                    updateTimers.put(updateEvent.delay(), System.currentTimeMillis() + updateEvent.delay());
                                }
                            }else{
                                lastRunTimers.put(updateEvent.delay(), System.currentTimeMillis() + updateEvent.delay());
                            }
                        }
                    }
                }
            }
        }

        for (var update : updateTimers.entrySet()) {
            lastRunTimers.put(update.getKey(), update.getValue());
        }
    }

    private void executeMethod(Method method, Object obj) {
        try {
            method.invoke(obj);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
