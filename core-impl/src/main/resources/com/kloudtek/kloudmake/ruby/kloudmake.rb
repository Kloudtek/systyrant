require "java"
java_import AbstractTask

module Kloudmake
  class Task < AbstractTask

  end

  def Kloudmake.create(type, id=nil, attrs=nil, parent=nil)
    $strm.create(type, id, attrs, parent)
  end
end