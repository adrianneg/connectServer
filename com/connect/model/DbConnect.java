package com.connect.model;

import java.sql.ResultSet;
import java.util.Properties;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbConnect{
    
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private PreparedStatement prepStmt ;
    private Properties auth = new Properties();
    
    public DbConnect(){
        try{
            auth.put("user", SECUREINFO.DBUSER);
            auth.put("password",SECUREINFO.DBPASSWORD);
            Class.forName("com.mysql.jdbc.Driver");
            
            connection =  DriverManager.getConnection("jdbc:mysql://localhost:8889/java_project",auth);
            if(connection != null){
                statement = connection.createStatement();
            }
        }catch(ClassNotFoundException ex){
           System.err.println("class not found in dBServer construct");
        } catch (SQLException ex) {
            System.err.println("mysql server down");
            //Logger.getLogger(DbConnect.class.getName()).log(Level.SEVERE, null, ex);
        }
         
    }
    
    public ArrayList<String> getMessageForUser(String username){
        try{
            
            
            String query = "SELECT * FROM users WHERE username='"+username+"'";//1000 in 1 sec so it's very fast
            //prepStmt = connection.prepareStatement(query);
            resultSet = statement.executeQuery(query);
            //prepStmt.setString(1, username);
            ArrayList<String> temp = new ArrayList<String>();
            //System.out.println(prepStmt.toString());
            //resultSet = prepStmt.executeQuery(query);
            resultSet = statement.executeQuery(query);
            //System.out.println("Records from database");
            while(resultSet.next()){
                //ArrayList temp1 = new ArrayList();
                String id = resultSet.getString("id");
                String user = resultSet.getString("username");
                String message = resultSet.getString("message");
                String message_type = resultSet.getString("message_type");
                Timestamp date = resultSet.getTimestamp("date");
                System.out.println(id +" "+ user + " "+ message + " "+ message_type + " "+ date.toString());
                temp.add(user + " "+ message + " "+ message_type + " "+ date.toString() + "\n");
                //String parish = resultSet.getString("blob");
                //System.out.println(id + " "+ user + " " + message +" " +message_type +" " + date.toLocalDate());
            }
            return temp;
        }catch(SQLException ex){
            System.err.println(ex);
        }
        finally{
            try {
                connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(DbConnect.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
        return null;
    }
    
    public void setUserMessage(String username, ChatMessage cm){
        try{
            
            //statement = connection.createStatement();
            long time = System.currentTimeMillis();
            java.sql.Timestamp timestamp = new java.sql.Timestamp(time);
            int numRowsAffected = 0;
            String query= "INSERT INTO messages_me( username, message, message_type, date)"
                    + " VALUES (?, ?, ?, ?, ?)";

            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, username);
            prepStmt.setString(2, (String) cm.getMessage());
            prepStmt.setString(3, "text");
            prepStmt.setTimestamp(4, timestamp);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();

            //numRowsAffected = sta.executeUpdate(prepStmt);
           if(numRowsAffected > 0){
                   System.out.println("Message added successfully");
                   return;

           }
        }catch(SQLException ex){
            System.err.println(ex);
        }
        finally{
            try {
                connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(DbConnect.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
    }

    public int validateUser(String username, String password){ 
    	//System.out.println("Validating on server database now...");
    	String query = "SELECT * FROM users WHERE username = ? AND password = md5(?)";
    	try {    		
                if(connection == null){
                    return 2;//if 2 then server database couldn't be connected to
                }
            
                prepStmt = connection.prepareStatement(query);
                prepStmt.setString(1, username);
                prepStmt.setString(2, password);

                //System.out.println(prepStmt.toString());
                resultSet= prepStmt.executeQuery();

                if(resultSet.next()){
                        System.out.println("Successfully Validated...");
                        return 1;//if 1 user password combination found --VALID USER
                }else{
                        System.out.println("ERROR user couldn't be Validated...");
                        return 0;//if 0 user password combination not found
                }
        } catch (SQLException e) {
                e.printStackTrace();
        }


        return 0;//if 0 user password combination not found
    	
    }

    public ArrayList<String> getFriendsOfUsername(String username){
    	ArrayList<String> arrayList = new ArrayList<>();
    	
    	String query = "SELECT * FROM friends WHERE ((fusername = ?) OR (username = ?)) AND accepted = true";
    	try { 
			prepStmt = connection.prepareStatement(query);
			prepStmt.setString(1, username);
			prepStmt.setString(2, username);
			//System.out.println(prepStmt.toString());
			resultSet= prepStmt.executeQuery();
			while(resultSet.next()){
				if(! resultSet.getString("fusername").equalsIgnoreCase(username)){
                                    //System.out.println(resultSet.getString("fusername"));
					arrayList.add(resultSet.getString("fusername"));
				}
				if(! resultSet.getString("username").equalsIgnoreCase(username)){
                                    //System.out.println(resultSet.getString("username"));
					arrayList.add(resultSet.getString("username"));
				}
				//System.out.println("Successfully Validated...");
			}
			if(arrayList.isEmpty()){
				//System.out.println("No friends available");
				return new ArrayList();
			}
                                //else{
//                            System.out.println("All friends");
//                            for(String f : arrayList){
//                                System.out.println(username + " friend is "+ f);
//                            }
//                        }
		} catch (SQLException e) {
			e.printStackTrace();
			return new ArrayList();
		}
    	return arrayList;
    }
    
    public boolean isAUser(String user){
    	String query = "SELECT username FROM users WHERE username = ?";
    	try {    		
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, user);

            resultSet= prepStmt.executeQuery();

            if(resultSet.next()){
                    System.out.println(user + " is a client in the database");
                    return true;
            }else{
                    System.out.println(user + " is not a client in the database");
                    return false;
            }
        } catch (SQLException e) {
                e.printStackTrace();
        }
        return false;
    }
   
    public boolean registerFriendRequest(String[] msgObj){
    	String requester =  msgObj[0];//current user
		String requestName =  msgObj[1];//user to be friended
		int numRowsAffected = 0;
    	String query = "INSERT INTO friends(username, fusername, accepted) VALUE( ?, ?, false)";
    	try{
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, requester);
            prepStmt.setString(2, requestName);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();

            if(numRowsAffected > 0){
                    System.out.println("Successfully sent a friend request...");
                    return true;
            }else{
                    System.out.println("ERROR sending a friend request...");
                    return false;
            }
        } catch (SQLException e) {
                System.out.println("SQL exception");
                e.printStackTrace();
        }
    	
    	return false;
    }

    public ArrayList<String> getAwaitingRequests(String username){
    	ArrayList<String> arrayList = new ArrayList<>();
    	   //System.out.println("got it "+ username);
    	//String query = "SELECT * FROM friends WHERE ((fusername = ?) OR (username = ?)) AND accepted = false";
        String query = "SELECT * FROM friends WHERE fusername = ? AND accepted = false";
    	try { 
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, username);
            //prepStmt.setString(2, username);
            //System.out.println(prepStmt.toString());
            resultSet= prepStmt.executeQuery();
            while(resultSet.next()){
//                    if(! resultSet.getString("fusername").equalsIgnoreCase(username)){
//                            arrayList.add(resultSet.getString("fusername"));
//                    }
              //  System.out.println(query);
                //System.out.println("username " +username);
                    if(!resultSet.getString("username").equalsIgnoreCase(username)){
                            arrayList.add(resultSet.getString("username"));
                    }
                    //System.out.println("Successfully Validated...");
            }
            if(arrayList.isEmpty()){
                    System.out.println("No friends available");
                    return null;
            }
        } catch (SQLException e) {
                return null;
        }
    	return arrayList;
    }

    public boolean updateRequest(String user, String clientUsername) {
        
	int numRowsAffected = 0;
    	String query = "UPDATE friends SET accepted = true WHERE username = ? AND fusername = ?";
    	try{
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, user);
            prepStmt.setString(2, clientUsername);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();

            if(numRowsAffected > 0){
                    System.out.println("Successfully accepted a friend request...");
                    return true;
            }else{
                    System.out.println("ERROR sending a friend request...");
                    return false;
            }
        } catch (SQLException e) {
                System.out.println("SQL exception");
                e.printStackTrace();
        }
    	
        return false;
    }
    
    public boolean updateGroupRequest(String membername, String group) {
        
	int numRowsAffected = 0;
    	String query = "UPDATE group_members SET accepted = ? WHERE groupname = ? AND membername = ? ";
    	try{
            prepStmt = connection.prepareStatement(query);
            prepStmt.setBoolean(1, true);
            prepStmt.setString(2, group);
            prepStmt.setString(3, membername);
            
               //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();

            if(numRowsAffected > 0){
                    //System.out.println("Successfully accepted a group request...");
                    return true;
            }else{
                    //System.out.println("ERROR sending a group request...");
                    return false;
            }
        } catch (SQLException e) {
                System.out.println("SQL exception");
                e.printStackTrace();
        }
    	
        return false;
    }

    public boolean deleteRequest(String user, String clientUsername) {
        int numRowsAffected = 0;
    	String query = "DELETE FROM friends WHERE accepted = false AND username = ? AND fusername = ?";
    	try{
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, user);
            prepStmt.setString(2, clientUsername);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();

            if(numRowsAffected > 0){
                    System.out.println("Successfully accepted a friend request...");
                    return true;
            }else{
                    System.out.println("ERROR sending a friend request...");
                    return false;
            }
        } catch (SQLException e) {
                System.out.println("SQL exception");
                e.printStackTrace();
        }
    	
        return false;
    }

    public boolean deleteFriend(String user, String clientUsername) {
        
        int numRowsAffected = 0;
    	
    	try{
            String query = "DELETE FROM friends WHERE accepted = true AND (username = ? AND fusername = ?) OR (username = ? AND fusername = ?)" ;
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, user);
            prepStmt.setString(2, clientUsername);
            prepStmt.setString(3, clientUsername);
            prepStmt.setString(4, user);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();

            if(numRowsAffected > 0 ){
                    System.out.println("Successfully deleted chat friend...");
                    return true;
            }else{
                    System.out.println("ERROR deleting chat friend...");
                    return false;
            }
        } catch (SQLException e) {
                System.out.println("SQL exception");
                e.printStackTrace();
        }
    	
        return false;
    }
    
    public int createGroup(String groupOwner, String groupName) {
        try{
            PreparedStatement prepStmt1, prepStmt2 ;
            if(connection == null){
                return 0;
            }
            int numRowsAffected, numRowsAffected1= 0;
            
            //check if groupname already exist
            String checkName = "SELECT groupname FROM groups WHERE groupname = ?";
            prepStmt2 = connection.prepareStatement(checkName);
            prepStmt2.setString(1, groupName);
            resultSet= prepStmt2.executeQuery();
            if(resultSet.next()){
                return 2;
            }
            
            //create group
            String query= "INSERT INTO groups( groupname, groupowner) VALUES (?, ?)";
            
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, groupName);
            prepStmt.setString(2, groupOwner);
            
            
            String query1= "INSERT INTO group_members( membername, groupname , accepted) VALUES (?, ?, ?)";
            //add self and part of the group
            prepStmt1 = connection.prepareStatement(query1);
            prepStmt1.setString(1, groupOwner);
            prepStmt1.setString(2, groupName);
            prepStmt1.setBoolean(3, true);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();
            numRowsAffected1= prepStmt1.executeUpdate();
            
            //numRowsAffected = sta.executeUpdate(prepStmt);
            //both actions must be true to create a group
           if(numRowsAffected > 0 && numRowsAffected1 > 0){
                   System.out.println("Message added successfully");
                   return 1;
           }else{
               //try to undo any one action that was performed and return false
               String query2= "DELETE FROM groups WHERE groupname = ? AND  groupowner = ? ";
                prepStmt1 = connection.prepareStatement(query2);
                prepStmt1.setString(1, groupName);
                prepStmt1.setString(2, groupOwner);
               
               String query3= "DELETE FROM group_members WHERE membername = ? AND  groupname = ? ";
                prepStmt1 = connection.prepareStatement(query3);
                prepStmt1.setString(1, groupOwner);
                prepStmt1.setString(2, groupName);
                
                System.err.println("Undo create group action because one/both quer/ies/y failed");
                //shouldn't matter if it successes
                return 0;//create not created
           }
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return 0;
    }
    
    public int registerUser(String fname, String lname, String username , String password) {
        try{
            PreparedStatement prepStmt1, prepStmt2 ;
            if(connection == null){
                return 2;//server down
            }
            int numRowsAffected;
            
            //check if user already exist
            String checkUser = "SELECT * FROM users WHERE username = ?";
            prepStmt2 = connection.prepareStatement(checkUser);
            prepStmt2.setString(1, username);
            
            resultSet = prepStmt2.executeQuery();
            if(resultSet.next()){
                //user exist
                return 3;
            }
            
            //add user
            String registerUser = "INSERT INTO users(fname, lname, username, password) VALUES(?, ?, ?, md5(?))";
            prepStmt1 = connection.prepareStatement(registerUser);
            prepStmt1.setString(1, fname);
            prepStmt1.setString(2, lname);
            prepStmt1.setString(3, username);
            prepStmt1.setString(4, password);
            
            numRowsAffected = prepStmt1.executeUpdate();
            if(numRowsAffected > 0){
                return 1;//successful register
            }else{
                return 0;//error
            }
            
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return 0;
    }
    
    public boolean addMemberToGroup(String memberName, String groupName) {
        try{
            
            if(connection == null){
                return false;
            }
            int numRowsAffected;
            //create group
            String query1= "INSERT INTO group_members( membername, groupname , accepted) VALUES (?, ?, ?)";
            //add self and part of the group
            prepStmt = connection.prepareStatement(query1);
            prepStmt.setString(1, memberName);
            prepStmt.setString(2, groupName);
            prepStmt.setBoolean(3, false);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();
            
            //numRowsAffected = sta.executeUpdate(prepStmt);
           if(numRowsAffected > 0 ){
                   System.out.println(memberName + " sent a request to join the "+ groupName +" group");
                   return true;
           }
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return false;
    }
    
    public boolean alreadyAMember(String memberName, String groupName) {
        try{
            
            if(connection == null){
                return false;
            }
            
            String query1= "SELECT * FROM group_members WHERE membername = ? AND groupname = ?";
            
            prepStmt = connection.prepareStatement(query1);
            prepStmt.setString(1, memberName);
            prepStmt.setString(2, groupName);

            //System.out.println(prepStmt.toString());
            resultSet = prepStmt.executeQuery();
            System.out.println(prepStmt.toString());
           if(resultSet.next()){
                   System.out.println("tryeeeee");
                   return true;
           }
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return false;
    }
  
    public ArrayList<String> getMembersOfGroup(String memberName, String groupName) {
        ArrayList<String> arrayList = new ArrayList<>();
        try{
            
            if(connection == null){
                return null;
            }
            //create group
            String query1= "SELECT membername FROM group_members WHERE membername !=  ? AND groupname = ? AND accepted = true";
            //add self and part of the group
            
            prepStmt = connection.prepareStatement(query1);
            prepStmt.setString(1, memberName);
            prepStmt.setString(2, groupName);

            //System.out.println(prepStmt.toString());
            
            resultSet= prepStmt.executeQuery();
            while(resultSet.next()){
                
                arrayList.add(resultSet.getString("membername"));
                   
            }
           if(arrayList.isEmpty()){
                    System.out.println("No members available");
                    return null;
            }
           return arrayList;
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return null;
    }

    public ArrayList<String> getGroupsRequest(String membername) {
        ArrayList<String> arrayList = new ArrayList<>();
        try{
            
            if(connection == null){
                return null;
            }
            //create group
            String query1= "SELECT * FROM group_members WHERE membername = ? AND accepted = false";
            //add self and part of the group
            prepStmt = connection.prepareStatement(query1);
            prepStmt.setString(1, membername);
            //System.out.println(prepStmt.toString());
            resultSet= prepStmt.executeQuery();
            while(resultSet.next()){
                arrayList.add(resultSet.getString("groupname"));
            }
           if(arrayList.isEmpty()){
                    System.out.println("No group request");
                    return null;
            }
           return arrayList;
           
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return null;
    }
    
    public ArrayList<String> getAllMyGroups(String membername) {
        ArrayList<String> arrayList = new ArrayList<>();
        try{
            
            if(connection == null){
                return null;
            }
            //create group
            String query1= "SELECT groupname FROM groups WHERE groupowner = ?";
            //add self and part of the group
            prepStmt = connection.prepareStatement(query1);
            prepStmt.setString(1, membername);
            //System.out.println(prepStmt.toString());
            resultSet= prepStmt.executeQuery();
            while(resultSet.next()){
                arrayList.add(resultSet.getString("groupname"));
            }
           if(arrayList.isEmpty()){
                    System.out.println("No group request");
                    return null;
            }
           return arrayList;
           
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return null;
    }
    
    public ArrayList<String> getAllGroupsImInButDontOwn(String membername) {
        ArrayList<String> arrayList = new ArrayList<>();
        try{
            
            if(connection == null){
                return null;
            }
            //create group
            String query1= "SELECT DISTINCT groupname FROM group_members "
                    + "WHERE membername = ? AND accepted = true AND groupname IN "
                    + "( SELECT groupname FROM groups WHERE groupowner != ? )";
            //add self and part of the group
            prepStmt = connection.prepareStatement(query1);
            prepStmt.setString(1, membername);
            prepStmt.setString(2, membername);
            //System.out.println(prepStmt.toString());
            resultSet= prepStmt.executeQuery();
            while(resultSet.next()){
                arrayList.add(resultSet.getString("groupname"));
            }
           if(arrayList.isEmpty()){
                    System.out.println("No group request");
                    return null;
            }
           return arrayList;
           
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return null;
    }
    
    public boolean deleteMemberFromGroup(String membername, String groupName) {
        int numRowsAffected = 0;
    	String query = "DELETE FROM group_members WHERE accepted = true AND groupname = ? AND membername = ?";
    	try{
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, groupName);
            prepStmt.setString(2, membername);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();

            if(numRowsAffected > 0){
                    System.out.println("Successfully deleted "+ membername +" from "+groupName+" group...");
                    return true;
            }else{
                    System.out.println("ERROR sending a deleting group member...");
                    return false;
            }
        } catch (SQLException e) {
                System.out.println("SQL exception");
                e.printStackTrace();
        }
    	
        return false;
    }

    public boolean deleteMemberGroupRequest(String membername, String groupName) {
        int numRowsAffected = 0;
    	String query = "DELETE FROM group_members WHERE accepted = false AND groupname = ? AND membername = ?";
    	try{
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, groupName);
            prepStmt.setString(2, membername);

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt.executeUpdate();

            if(numRowsAffected > 0){
                    System.out.println("Successfully delete joing group request...");
                    return true;
            }else{
                    System.out.println("ERROR deleting a join group request...");
                    return false;
            }
        } catch (SQLException e) {
                System.out.println("SQL exception");
                e.printStackTrace();
        }
    	
        return false;
    }

    public boolean deleteGroup(String groupOwner, String groupName) {
        int numRowsAffected = 0, numRowsAffected1 = 0;
        
        PreparedStatement prepStmt1 ;
        
    	try{
            String query1 = "DELETE FROM groups WHERE groupname = ? AND groupowner = ?";
    	
            prepStmt = connection.prepareStatement(query1);
            prepStmt.setString(1, groupName);
            prepStmt.setString(2, groupOwner);

            String query = "DELETE FROM group_members WHERE groupname = ?";
            prepStmt1 = connection.prepareStatement(query);
            prepStmt1.setString(1, groupName);
            
            //System.out.println(prepStmt.toString());
            numRowsAffected = prepStmt.executeUpdate();
            numRowsAffected1 = prepStmt1.executeUpdate();
                    
            if(numRowsAffected > 0 || numRowsAffected1  > 0){                
                    System.out.println("Successfully deleted group and all member...");
                    return true;
            }else{
                    System.out.println("ERROR deleting group...");
                    return false;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
                System.out.println("SQL exception");
        }
    	
        return false;
    }
    
    public boolean addMessage(String from, String to, String message){
        try{
            PreparedStatement prepStmt1;
            if(connection == null){
                return false;
            }
            int numRowsAffected;
            String query1 = "INSERT INTO messages( mfrom, mto , message) VALUES (?, ?, ?)";
            //add self and part of the group
            prepStmt1 = connection.prepareStatement(query1);
            prepStmt1.setString(1, from);
            prepStmt1.setString(2, to);
            prepStmt1.setString(3, message.trim());

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt1.executeUpdate();

           if(numRowsAffected > 0){
                   System.out.println("Message added successfully");
                   return true;
           }
        } catch(SQLException sql){
            sql.printStackTrace();
        }
       return false;
    }
    
    public boolean addGroupMessage(String from, String groupname, String message){
        try{
            PreparedStatement prepStmt1;
            if(connection == null){
                return false;
            }
            int numRowsAffected;
            String query1 = "INSERT INTO group_messages( mfrom, groupname , message) VALUES (?, ?, ?)";
            //add self and part of the group
            prepStmt1 = connection.prepareStatement(query1);
            prepStmt1.setString(1, from);
            prepStmt1.setString(2, groupname);
            prepStmt1.setString(3, message.trim());

            //System.out.println(prepStmt.toString());
            numRowsAffected= prepStmt1.executeUpdate();

           if(numRowsAffected > 0){
                   System.out.println("Message added successfully");
                   return true;
           }
        } catch(SQLException sql){
            sql.printStackTrace();
        }
       return false;
    }

    public ArrayList<String[]> getAllMessageOfGroup(String groupname){
       
        ArrayList<String[]> arrayList = new ArrayList<>();
        try{
            if(connection == null){
                return null;
            }
            String q = "SELECT * FROM group_messages WHERE groupname = ?";
            prepStmt = connection.prepareStatement(q);
            prepStmt.setString(1, groupname);
            resultSet= prepStmt.executeQuery();
            
            while(resultSet.next()){
                //each represents a message
                arrayList.add(new String[]{resultSet.getString("mfrom"),resultSet.getString("message")});
            }
           if(arrayList.isEmpty()){
                    System.out.println("No group request");
                    return null;
            }
           return arrayList;
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return null;
    }

    public ArrayList<String[]> getAllMessage(String from, String to){
       
        ArrayList<String[]> arrayList = new ArrayList<>();
        try{
            
            if(connection == null){
                return null;
            }
            String q = "SELECT * FROM messages WHERE (mfrom = ? and mto = ?) OR (mfrom = ? and mto = ? ) ORDER BY id ASC";
            prepStmt = connection.prepareStatement(q);
            prepStmt.setString(1, from);
            prepStmt.setString(2, to);
            prepStmt.setString(3, to);
            prepStmt.setString(4, from);
            resultSet= prepStmt.executeQuery();
            
            while(resultSet.next()){
                //each represents a message
                arrayList.add(new String[]{resultSet.getString("mfrom"),resultSet.getString("message")});
            }
           if(arrayList.isEmpty()){
                System.out.println("No group request");
                return null;
            }
           return arrayList;
        }catch(SQLException ex){
            System.err.println(ex);
        }
        return null;
    }
}
