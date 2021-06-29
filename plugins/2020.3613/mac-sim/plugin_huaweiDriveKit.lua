local Library = require "CoronaLibrary"

local lib = Library:new{ name='plugin.huaweiDriveKit', publisherId='com.solar2d' }

local placeholder = function()
	print( "WARNING: The '" .. lib.name .. "' library is not available on this platform." )
end

lib.init = placeholder
lib.createDirectory = placeholder
lib.createFile = placeholder
lib.updateFile = placeholder
lib.deleteFile = placeholder
lib.downloadFile = placeholder
lib.getFileList = placeholder
lib.createComments = placeholder
lib.listComments = placeholder
lib.getComments = placeholder
lib.updateComments = placeholder
lib.deleteComments = placeholder
lib.createReplies = placeholder
lib.listReplies = placeholder
lib.getReplies = placeholder
lib.updateReplies = placeholder
lib.deleteReplies = placeholder
lib.listHistoryVersions = placeholder
lib.getHistoryVersions = placeholder
lib.deleteHistoryVersions = placeholder

-- Return an instance
return lib