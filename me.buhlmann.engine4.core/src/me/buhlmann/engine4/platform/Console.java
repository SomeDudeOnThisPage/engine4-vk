package me.buhlmann.engine4.platform;

import org.joml.Vector3f;

import java.io.File;
import java.util.*;

public class Console
{
    public static final class ConVar<T>
    {
        private final Class<?> type;
        private T value;

        public T get()
        {
            return this.value;
        }

        public void set(Object value)
        {
            this.value = (T) value;
        }

        public Class<?> type()
        {
            return this.type;
        }

        public ConVar(T value)
        {
            this.type = value.getClass();
            this.value = value;
        }
    }

    public interface ConCommand
    {
        void run(String[] args) throws Exception;
    }

    public static Console instance;

    public static Console getInstance()
    {
        if (Console.instance == null)
        {
            Console.instance = new Console();
        }

        return Console.instance;
    }

    public enum MessageType
    {
        DEFAULT, WARNING, ERROR
    }

    public static final Vector3f COLOR_DEFAULT = new Vector3f(1.0f, 1.0f, 1.0f);
    public static final Vector3f COLOR_ERROR = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3f COLOR_WARNING = new Vector3f(1.0f, 1.0f, 0.0f);

    private final Map<String, ConVar<?>> convars;
    private final Map<String, ConCommand> commands;

    private final List<AbstractMap.SimpleEntry<String, MessageType>> history;
    private int size;

    public static void parse(Object parse)
    {
        String input = parse.toString();

        Console console = Console.getInstance();
        String[] tokens = input.split(" ");
        String command = tokens[0];

        if (console.commands.containsKey(command))
        {
            // execute command
            try
            {
                console.commands.get(command).run(Arrays.copyOfRange(tokens, 1, tokens.length));
            }
            catch(Exception e)
            {
                Console.print(e.toString());
            }
            return;
        }
        else if (console.convars.containsKey(command))
        {
            // set convar
            Class<?> type = console.convars.get(command).type();
            Object data = null;

            if (tokens.length < 2)
            {
                Console.error("data type mismatch - expected '" + type.getSimpleName() + "', got none");
                return;
            }

            try
            {
                if (type.equals(Float.class))
                {
                    data = Float.parseFloat(tokens[1]);
                }
                else if (type.equals(Double.class))
                {
                    data = Double.parseDouble(tokens[1]);
                }
                else if (type.equals(Integer.class))
                {
                    data = Integer.parseInt(tokens[1]);
                }
                else if (type.equals(Boolean.class))
                {
                    data = tokens[1].equalsIgnoreCase("true");

                    if (!tokens[1].equalsIgnoreCase("false") && !(Boolean) data)
                    {
                        // check for 0 / 1
                        int i = Integer.parseInt(tokens[1]);
                        switch (i)
                        {
                            case 0 -> data = false;
                            case 1 -> data = true;
                            default -> throw new NumberFormatException("data type mismatch - expected '" + type + "'");
                        }
                    }
                }
                else if (type.equals(String.class))
                {
                    data = tokens[1];
                }

                if (data == null)
                {
                    throw new Exception("data type mismatch - expected '" + type + "'");
                }

                console.convars.get(command).set(data);
                Console.print("set '" + command + "' to '" + data + "'");
                return;
            }
            catch(Exception e)
            {
                if (e instanceof NumberFormatException)
                {
                    Console.error("data type mismatch - expected '" + type.getSimpleName() + "'");
                }
                else
                {
                    Console.error(e.toString());
                }
                return;
            }
        }

        Console.warning("'" + command + "' is not a recognized operation");
    }

    private static void append(MessageType type, Object... message)
    {
        Console console = Console.getInstance();
        for (Object msg : message)
        {
            String string = msg.toString();
            if (console.history.size() >= console.size)
            {
                console.history.remove(0);
            }

            console.history.add(new AbstractMap.SimpleEntry<>(string, type));
        }
    }

    public static void warning(Object... message)
    {
        Console.append(MessageType.WARNING, message);
    }

    public static void error(Object... message)
    {
        Console.append(MessageType.ERROR, message);
    }

    public static void print(Object... message)
    {
        Console.append(MessageType.DEFAULT, message);
    }

    public static List<AbstractMap.SimpleEntry<String, MessageType>> history()
    {
        return Console.getInstance().history;
    }

    public static void addConVar(String key, ConVar<?> var)
    {
        Console.getInstance().convars.put(key, var);
    }

    public static void addCommand(String key, ConCommand command)
    {
        Console.getInstance().commands.put(key, command);
    }

    public static ConVar<?> getConVar(String key)
    {
        Console console = Console.getInstance();
        if (!console.convars.containsKey(key))
        {
            throw new UnsupportedOperationException("convar '" + key + "' does not exist");
        }
        return console.convars.get(key);
    }

    public static void exec(String cfg)
    {
        // todo: search default cfg path, and all specified in engine.toml / instance.toml files
        // for now just use default ./platform/cfg directory

        Console.print("executing '" + cfg + ".cfg'");

        // load file and execute line by line
        try
        {
            File file = new File("platform/cfg/" + cfg + ".cfg");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine())
            {
                String current = scanner.nextLine().trim();
                if (!current.startsWith("#") && !current.equals(""))
                {
                    Console.parse(current);
                }
            }
            scanner.close();
        }
        catch(Exception e)
        {
            Console.error("could not find file './platform/cfg/" + cfg + ".cfg'");
        }
    }

    public static void clear()
    {
        Console.getInstance().history.clear();
    }

    private Console()
    {
        this.history = new ArrayList<>();
        this.size = 100;

        this.convars = new HashMap<>();
        this.commands = new HashMap<>();

        this.commands.put("c_console_history_size", (args) -> {
            int size = Integer.parseInt(args[0]);
            if (size > 0)
            {
                if (this.history.size() > size)
                {
                    this.history.subList(0, this.history.size() - size).clear();
                }
                this.size = size;
            }
        });

        this.commands.put("ping", (args) -> Console.print("pong"));
        this.commands.put("clear", (args) -> Console.clear());
        this.commands.put("exec", (args) -> Console.exec(args[0]));

        this.convars.put("editor_test", new ConVar<>(0));
    }
}
