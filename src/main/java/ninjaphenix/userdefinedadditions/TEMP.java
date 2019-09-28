package ninjaphenix.userdefinedadditions;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.impl.SyntaxError;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import ninjaphenix.userdefinedadditions.readers.ReaderManager;
import ninjaphenix.userdefinedadditions.readers.interfaces.Reader;
import ninjaphenix.userdefinedadditions.readers.main.ItemReaderV0;
import ninjaphenix.userdefinedadditions.readers.reusable.CustomFoodComponentReaderV0;
import ninjaphenix.userdefinedadditions.readers.reusable.ItemSettingsReaderV0;
import ninjaphenix.userdefinedadditions.readers.reusable.PredefinedFoodComponentReaderV0;
import ninjaphenix.userdefinedadditions.readers.reusable.ReaderReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TEMP
{
    public static Map<Identifier, ItemGroup> itemGroups = new HashMap<>();
    private static TEMP instance;
    private final String MOD_ID = "userdefinedadditions";
    private final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDirectory().toPath().resolve(MOD_ID);
    private final Jankson jankson;

    private TEMP()
    {
        Jankson.Builder builder = new Jankson.Builder();
        builder.registerSerializer(Identifier.class, (id, marshaller) -> new JsonPrimitive(id.toString()));
        builder.registerPrimitiveTypeAdapter(Identifier.class, (it) -> (it instanceof String) ? new Identifier((String) it) : new Identifier(it.toString()));
        // todo add api for others to expand on json content system. Including adding their own deserializers
        jankson = builder.build();
        instance = this;

        ReaderManager manager = ReaderManager.getInstance();
        manager.registerSerializerType(new Identifier("minecraft", "item"), 0, ItemReaderV0.getInstance());
        manager.registerSerializerType(new Identifier("minecraft", "item_settings"), 0, ItemSettingsReaderV0.getInstance());
        manager.registerSerializerType(new Identifier("minecraft", "predefined_food_component"), 0, PredefinedFoodComponentReaderV0.getInstance());
        manager.registerSerializerType(new Identifier("minecraft", "food_component"), 0, CustomFoodComponentReaderV0.getInstance());
    }

    @SuppressWarnings("WeakerAccess")
    public static TEMP getInstance()
    {
        if (instance == null) instance = new TEMP();
        return instance;
    }

    private void initialize(Path path)
    {
        if (Files.isDirectory(path))
        {
            final String mod_id = path.getFileName().toString();
            try
            {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>()
                {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                    {
                        String filename = file.getFileName().toString();
                        if (filename.endsWith(".json"))
                        {
                            final Identifier id = new Identifier(mod_id, filename.substring(0, filename.length() - 5));
                            try
                            {
                                LOGGER.info("Reading: {}", id); // REMOVE THIS
                                JsonObject object = jankson.load(Files.newInputStream(file, StandardOpenOption.READ));
                                ReaderReader.Data d = ReaderReader.getInstance().read(object);
                                LOGGER.info("    wants to be deserialized with: {}, v={}", d.getType(), d.getVersion());
                                Reader<?, ?> serializer = ReaderManager.getInstance().getSerializer(d.getType(), d.getVersion());
                                if (serializer == null)
                                {
                                    LOGGER.info("       read: serializer not found");
                                }
                                else
                                {
                                    LOGGER.info("        read: {}", serializer.read(d.get()));
                                    LOGGER.info("        get: {}", serializer.read(d.get()).get());
                                }
                            }
                            catch (IOException e)
                            {
                                LOGGER.warn("Skipping {} due to read error.", id);
                            }
                            catch (SyntaxError e)
                            {
                                LOGGER.warn("Skipping {} due to invalid json detected.", id);
                                LOGGER.throwing(e);
                            }
                            catch (RuntimeException e)
                            {
                                LOGGER.warn("Skipping {} due to wrong contents.", id);
                                LOGGER.throwing(e);
                            }
                        }

                        return super.visitFile(file, attrs);
                    }
                });
            }
            catch (IOException e)
            {
                LOGGER.error("File parsing interrupted, visiting a file or directory failed.");
            }
        }
    }

    public void onInitialize()
    {
        if (mkDirSafely(CONFIG_DIR))
        {
            try
            {
                Iterator<Path> paths = Files.list(CONFIG_DIR).iterator();
                while (paths.hasNext()) initialize(paths.next());
            }
            catch (IOException e)
            {
                LOGGER.error("Failed to list sub directories of {}.", CONFIG_DIR);
            }
        }
    }

    /*
        Returns true if directory exists, false otherwise.
     */
    private boolean mkDirSafely(Path dir)
    {
        if (!Files.exists(dir))
        {
            try
            {
                Files.createDirectories(dir);
            }
            catch (IOException e)
            {
                LOGGER.error("[{}] Failed to create directory: {}", MOD_ID, dir);
                LOGGER.throwing(e);
                return false;
            }
        }
        return Files.isDirectory(dir);
    }

    private boolean dirExists(Path dir)
    {
        return Files.exists(dir) && Files.isDirectory(dir);
    }
}
