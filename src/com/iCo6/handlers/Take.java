package com.iCo6.handlers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;

import com.iCo6.command.Handler;
import com.iCo6.command.Parser.Argument;
import com.iCo6.command.exceptions.InvalidUsage;

import com.iCo6.Constants;
import com.iCo6.iConomy;
import com.iCo6.system.Account;
import com.iCo6.system.Accounts;

import com.iCo6.util.Messaging;
import com.iCo6.util.Template;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Take extends Handler {
    private Accounts Accounts = new Accounts();

    public Take(iConomy plugin) {
        super(plugin, plugin.Template);
    }

    @Override
    public boolean perform(CommandSender sender, LinkedHashMap<String, Argument> arguments) throws InvalidUsage {
        if(!hasPermissions(sender, "take"))
            throw new InvalidUsage("You do not have permission to do that.");

        String name = arguments.get("name").getStringValue();
        String tag = template.color(Template.Node.TAG_MONEY);
        Double amount;

        if(name.equals("0"))
            throw new InvalidUsage("Missing name parameter: /money take <name> <amount>");

        if(arguments.get("amount").getStringValue().equals("empty"))
            throw new InvalidUsage("Missing amount parameter: /money take <name> <amount>");

        try {
            amount = arguments.get("amount").getDoubleValue();
        } catch(NumberFormatException e) {
            throw new InvalidUsage("Invalid amount parameter, must be double.");
        }

        if(Double.isInfinite(amount) || Double.isNaN(amount))
            throw new InvalidUsage("Invalid amount parameter, must be double.");

        if(!Accounts.exists(name)) {
            template.set(Template.Node.ERROR_ACCOUNT);
            template.add("name", name);

            Messaging.send(sender, tag + template.parse());
            return false;
        }
        
        // Logging to DB
        dbPayLog((Player)sender, name, amount);

        Account account = new Account(name);
        account.getHoldings().subtract(amount);

        template.set(Template.Node.PLAYER_DEBIT);
        template.add("name", name);
        template.add("amount", iConomy.format(amount));

        Messaging.send(sender, tag + template.parse());
        return false;
    }
    
    public void dbPayLog( Player from, String to, Double amount ) {
    	
    	Connection conn = null;
    	
        try
        {
            String userName = Constants.Nodes.DatabaseUsername.toString();
            String password = Constants.Nodes.DatabasePassword.toString();
            String url = "jdbc:" + Constants.Nodes.DatabaseUrl.toString();
            Class.forName ("com.mysql.jdbc.Driver").newInstance ();
            conn = DriverManager.getConnection (url, userName, password);
            //System.out.println ("Database connection established");
        }
        catch (Exception e)
        {
            System.err.println ("Cannot connect to database server");
        }
        finally
        {
            if (conn != null)
            {
               
            	Statement s;
				try {
					
					s = conn.createStatement();
					s.execute("INSERT INTO `iConomyLog` (`id`, `command`, `from`, `to`, `amount`, `date`) VALUES (NULL, 'take', '" + from.getName() +"', '" + to + "', '" + amount + "', CURRENT_TIMESTAMP);");
					s.close();
	                conn.close ();
	                //System.out.println ("Database connection terminated");
	                
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.err.println ("Error message: " + e.getMessage ());
					System.err.println ("Error number: " + e.getErrorCode ());
				}
            }
        }
    }
}
