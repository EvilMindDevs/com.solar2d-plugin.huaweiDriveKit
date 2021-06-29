# Huawei Drive Kit Solar2d Plugin

This plugin was created based on Huawei Drive Kit. Please [Drive Kit](https://developer.huawei.com/consumer/en/hms/huawei-drivekit/) for detailed information. 

In order to use this plugin, you must first create an account from developer.huawei.com. And after logging in with your account, you must create a project in the huawei console in order to use HMS kits.

## Project Setup

To use the plugin please add following to `build.settings`

```lua
{
    plugins = {
        ["plugin.huaweiDriveKit"] = {
            publisherId = "com.solar2d",
        },
    },
}
```

And then you have to create keystore for your app. And you must generate sha-256 bit fingerprint from this keystore using the command here. You have to define this fingerprint to your project on the huawei console.

And you must add the keystore you created while building your project.
Also you need to give the package-name of the project you created on Huawei Console.
And you need to put `agconnect-services.json` file into `main.lua` directory.

After all the configuration processes, you must define the plugin in main.lua.

```lua
local huaweiDriveKit = require "plugin.huaweiDriveKit"

local function listener(event)
    print(event)
end

huaweiDriveKit.init(listener, {unionId="xxxx", accessToken="xxxx"}, function(event) 
    print(json.prettify( event ))
end) -- sets listener and inits plugin
```

As you can see above, you we can get result of methods with listener method. 

## Methods in the Plugin
# Drive Kit
# File Operations
### createDirectory
Call the createDirectory method to create a directory.
```lua
library.createDirectory({dirName="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = createDirectory (text)
      provider = DriveKit (text)
      data = Directory(json)
} 
]]--
```
### updateFile
Call the updateFile method to update file or folder.

```lua
library.updateFile({fileId="xxxx", newFilePath="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = updateFile (text)
      provider = DriveKit (text)
} 
]]--
```

### deleteFile
Call the deleteFile method to delete a file or folder permanently.

```lua
library.deleteFile({fileId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = deleteFile (text)
      provider = DriveKit (text)
} 
]]--
```

### downloadFile
Call the downLoadFile method to delete a file or folder permanently.

```lua
library.downloadFile({fileId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = downloadFile (text)
      provider = DriveKit (text)
} 
]]--
```


### getFileList
Call the getFileList method to search for files.

```lua
library.getFileList({query="xxxx", orderBy="xxxx", fields="xxxx", pageSize=2}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = getFileList (text)
      provider = DriveKit (text)
      data = Files (jsonArray)
} 
]]--
```
# Comment Operations

### createComments
Call the createComments method to create a comment.


```lua
library.createDirectory({fileId="xxxx", description="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = createComments (text)
      provider = DriveKit (text)
      data = Comment(json)
} 
]]--
```

### listComments
Call the listComments method to list comments.


```lua
library.listComments({fileId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = listComments (text)
      provider = DriveKit (text)
      data = Comments(jsonArray)
} 
]]--
```
### getComments
Call the getComments method to obtain comment details.


```lua
library.getComments({fileId="xxxx", commentId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = getComments (text)
      provider = DriveKit (text)
      data = Comment(json)
} 
]]--
```

### updateComments
Call the updateComments method to update a comment.


```lua
library.updateComments({fileId="xxxx", commentId="xxxx", description="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = updateComments (text)
      provider = DriveKit (text)
} 
]]--
```

### deleteComments
Call the deleteComments method to obtain comment details.


```lua
library.deleteComments({fileId="xxxx", commentId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = deleteComments (text)
      provider = DriveKit (text)
} 
]]--
```


# Reply Operations

### createReplies
Call the createReplies method to create a reply.

```lua
library.createReplies({fileId="xxxx", description="xxxx", description="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = createReplies (text)
      provider = DriveKit (text)
      data = Reply(json)
} 
]]--
```

### listReplies
Call the listReplies method to list replies.


```lua
library.listReplies({fileId="xxxx", commentId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = listReplies (text)
      provider = DriveKit (text)
      data = Replies(jsonArray)
} 
]]--
```
### getReplies
Call the getReplies method to obtain reply details.


```lua
library.getReplies({fileId="xxxx", commentId="xxxx", replyId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = getReplies (text)
      provider = DriveKit (text)
      data = Reply(json)
} 
]]--
```

### updateReplies
Call the updateReplies method to update a reply.

```lua
library.updateReplies({fileId="xxxx", commentId="xxxx", description="xxxx", description="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = updateReplies (text)
      provider = DriveKit (text)
} 
]]--
```

### deleteReplies
Call the deleteReplies method to obtain comment details.


```lua
library.deleteReplies({fileId="xxxx", commentId="xxxx", replyId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = deleteReplies (text)
      provider = DriveKit (text)
} 
]]--
```


# Historical Versions Operations

### listHistoryVersions
Call the listHistoryVersions method to list the historical versions of a file.

```lua
library.listHistoryVersions({fileId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = listHistoryVersions (text)
      provider = DriveKit (text)
      data = HistoryVersionList(json)
} 
]]--
```

### getHistoryVersions
Call the getHistoryVersions method to obtain the historical version details of a file.
```lua
library.getHistoryVersions({fileId="xxxx", historyVersionId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = getHistoryVersions (text)
      provider = DriveKit (text)
      data = HistoryVersion(json)
} 
]]--
```


### deleteHistoryVersions
Call the deleteHistoryVersions method to delete a historical version of a file.

```lua
library.deleteHistoryVersions({fileId="xxxx", historyVersionId="xxxx"}, function(event) 
    print(json.prettify( event ))
end)

--Result 
--[[(Listener) Table {
      isError = true|false
      message = text
      type = deleteHistoryVersions (text)
      provider = DriveKit (text)
} 
]]--
```

## References
HMS Drive Kit [Check](https://developer.huawei.com/consumer/en/hms/huawei-drivekit/)

## License
MIT
