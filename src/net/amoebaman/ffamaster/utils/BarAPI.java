
package net.amoebaman.ffamaster.utils;

import java.lang.reflect.*;
import java.util.*;

import org.bukkit.*;
import org.bukkit.entity.*;

public class BarAPI {

    private static Map<String, FakeDragon> dragonMap = new HashMap<String, FakeDragon>();

    public static void setBar(Player player, String text, int percent, boolean reset) {

        String playerName = player.getName();
        FakeDragon dragon = dragonMap.containsKey(playerName) ? dragonMap
                .get(playerName) : null;

                if (reset || text.isEmpty()) {
                    sendPacket(player, dragon.getDestroyPacket());
                    dragonMap.remove(playerName);
                }

                if (reset) {
                    dragon = new FakeDragon(player.getLocation().add(0, -200, 0), text, percent);
                    sendPacket(player, dragon.getSpawnPacket());
                    dragonMap.put(playerName, dragon);
                }
                else {
                    dragon.setName(text);
                    dragon.setHealth(percent);
                    sendPacket(player, dragon.getMetaPacket(dragon.getWatcher()));
                    sendPacket(player, dragon.getTeleportPacket(player.getLocation().add(0, -200, 0)));
                }
    }

    private static void sendPacket(Player player, Object packet) {
        try {
            Object nmsPlayer = ReflectionUtils.getHandle(player);
            Field connectionField = nmsPlayer.getClass().getField("playerConnection");
            Object connection = connectionField.get(nmsPlayer);
            Method sendPacket = ReflectionUtils.getMethod(connection.getClass(), "sendPacket");
            sendPacket.invoke(connection, packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class FakeDragon {

        private static final int MAX_HEALTH = 200;
        private int              id;
        private int              x;
        private int              y;
        private int              z;
        private int              pitch      = 0;
        private int              yaw        = 0;
        private byte             xvel       = 0;
        private byte             yvel       = 0;
        private byte             zvel       = 0;
        private float            health;
        private boolean          visible    = false;
        private String           name;
        private Object           world;

        private Object           dragon;

        public FakeDragon(Location loc, String name, int percent) {
            this.name = name;
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            this.health = percent / 100F * MAX_HEALTH;
            this.world = ReflectionUtils.getHandle(loc.getWorld());
        }

        public void setHealth(int percent) {
            this.health = percent / 100F * MAX_HEALTH;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getSpawnPacket() {
            Class<?> Entity = ReflectionUtils.getCraftClass("Entity");
            Class<?> EntityLiving = ReflectionUtils.getCraftClass("EntityLiving");
            Class<?> EntityEnderDragon = ReflectionUtils.getCraftClass("EntityEnderDragon");

            try{
                dragon = EntityEnderDragon.getConstructor(ReflectionUtils.getCraftClass("World")).newInstance(world);

                ReflectionUtils.getMethod(EntityEnderDragon, "setLocation", double.class, double.class, double.class, float.class, float.class).invoke(dragon, x, y, z, pitch, yaw);
                ReflectionUtils.getMethod(EntityEnderDragon, "setInvisible", boolean.class).invoke(dragon, visible);
                ReflectionUtils.getMethod(EntityEnderDragon, "setCustomName", String.class ).invoke(dragon, name);
                ReflectionUtils.getMethod(EntityEnderDragon, "setHealth", float.class).invoke(dragon, health);

                ReflectionUtils.getField(Entity, "motX").set(dragon, xvel);
                ReflectionUtils.getField(Entity, "motY").set(dragon, yvel);
                ReflectionUtils.getField(Entity, "motZ").set(dragon, zvel);

                this.id = (Integer) ReflectionUtils.getMethod(EntityEnderDragon, "getId").invoke(dragon);

                Class<?> packetClass = ReflectionUtils.getCraftClass("PacketPlayOutSpawnEntityLiving");
                return packetClass.getConstructor(new Class<?>[]{ EntityLiving }).newInstance(dragon);
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }

        public Object getDestroyPacket(){
            try{
                Class<?> packetClass = ReflectionUtils.getCraftClass("PacketPlayOutEntityDestroy");
                return packetClass.getConstructors()[0].newInstance(id);
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }

        public Object getMetaPacket(Object watcher){
            try{
                Class<?> watcherClass = ReflectionUtils.getCraftClass("DataWatcher");
                Class<?> packetClass = ReflectionUtils.getCraftClass("PacketPlayOutEntityMetadata");
                return packetClass.getConstructor(new Class<?>[] { int.class, watcherClass, boolean.class }).newInstance(id, watcher, true);
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }

        public Object getTeleportPacket(Location loc){
            try{
                Class<?> packetClass = ReflectionUtils.getCraftClass("PacketPlayOutEntityTeleport");
                return packetClass.getConstructor(new Class<?>[] { int.class, int.class, int.class, int.class, byte.class, byte.class }).newInstance(
                        this.id, loc.getBlockX() * 32, loc.getBlockY() * 32, loc.getBlockZ() * 32, (byte) ((int) loc.getYaw() * 256 / 360), (byte) ((int) loc.getPitch() * 256 / 360));
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }

        public Object getWatcher(){
            Class<?> Entity = ReflectionUtils.getCraftClass("Entity");
            Class<?> DataWatcher = ReflectionUtils.getCraftClass("DataWatcher");

            try{
                Object watcher = DataWatcher.getConstructor(new Class<?>[] { Entity }).newInstance(dragon);
                Method a = ReflectionUtils.getMethod(DataWatcher, "a", new Class<?>[] { int.class, Object.class });

                a.invoke(watcher, 0, visible ? (byte) 0 : (byte) 0x20);
                a.invoke(watcher, 6, (Float) health);
                a.invoke(watcher, 7, (Integer) 0);
                a.invoke(watcher, 8, (Byte) (byte) 0);
                a.invoke(watcher, 10, name);
                a.invoke(watcher, 11, (Byte) (byte) 1);
                return watcher;
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }

    }

    public static class ReflectionUtils {

        public static void sendPacket(List<Player> players, Object packet) {
            for (Player p : players) {
                sendPacket(p, packet);
            }
        }

        public static void sendPacket(Player p, Object packet) {
            try {
                Object nmsPlayer = getHandle(p);
                Field con_field = nmsPlayer.getClass().getField("playerConnection");
                Object con = con_field.get(nmsPlayer);
                Method packet_method = getMethod(con.getClass(), "sendPacket");
                packet_method.invoke(con, packet);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static Class<?> getCraftClass(String ClassName) {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            String version = name.substring(name.lastIndexOf('.') + 1) + ".";
            String className = "net.minecraft.server." + version + ClassName;
            Class<?> c = null;
            try {
                c = Class.forName(className);
            }
            catch (Exception e) { e.printStackTrace(); }
            return c;
        }

        public static Object getHandle(Entity entity) {
            Object nms_entity = null;
            Method entity_getHandle = getMethod(entity.getClass(), "getHandle");
            try {
                nms_entity = entity_getHandle.invoke(entity);
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return nms_entity;
        }

        public static Object getHandle(World world) {
            Object nms_entity = null;
            Method entity_getHandle = getMethod(world.getClass(), "getHandle");
            try {
                nms_entity = entity_getHandle.invoke(world);
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return nms_entity;
        }

        public static Field getField(Class<?> cl, String field_name) {
            try {
                Field field = cl.getDeclaredField(field_name);
                return field;
            }
            catch (SecurityException e) {
                e.printStackTrace();
            }
            catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            return null;
        }

        public static Method getMethod(Class<?> cl, String method, Class<?>... args) {
            for (Method m : cl.getMethods()) {
                if (m.getName().equals(method)
                        && ClassListEqual(args, m.getParameterTypes())) {
                    return m;
                }
            }
            return null;
        }

        public static Method getMethod(Class<?> cl, String method, Integer args) {
            for (Method m : cl.getMethods()) {
                if (m.getName().equals(method)
                        && args.equals(Integer.valueOf(m.getParameterTypes().length))) {
                    return m;
                }
            }
            return null;
        }

        public static Method getMethod(Class<?> cl, String method) {
            for (Method m : cl.getMethods()) {
                if (m.getName().equals(method)) {
                    return m;
                }
            }
            return null;
        }

        public static boolean ClassListEqual(Class<?>[] l1, Class<?>[] l2) {
            boolean equal = true;

            if (l1.length != l2.length)
                return false;
            for (int i = 0; i < l1.length; i++) {
                if (l1[i] != l2[i]) {
                    equal = false;
                    break;
                }
            }

            return equal;
        }
    }
}