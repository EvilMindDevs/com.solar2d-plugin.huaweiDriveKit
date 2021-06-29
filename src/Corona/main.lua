local huaweiDriveKit = require "plugin.huaweiDriveKit"
local widget = require( "widget" )
local json = require("json")

local function listener( event )
	print( "HMS Drive Kit " , json.prettify(event))
end

huaweiDriveKit.init(listener, {unionId="selam", accessToken="selam"}, function(event) 
                print(json.prettify( event ))
            end)

local createDirectory = widget.newButton(
    {
        left = 55,
        top = 195,
        id = "createDirectory",
        label = "createDirectory",
        onPress = function(event)
            huaweiDriveKit.createDirectory({dirName="xxxx"}, function(event) 
                print(json.prettify( event ))
            end)
        end,
        width = 210,
        height = 30
    }
)
