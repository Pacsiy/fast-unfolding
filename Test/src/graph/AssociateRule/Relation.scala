package graph.AssociateRule

import java.sql.Timestamp

class Relation(val time: Timestamp, val message_type: Int, val src: Long, val dest: Long, typenum:Int, val marks: Int) extends Serializable{
  val id:Long = getId();
  def getId():Long={
    var tmp:Long = 0
    if(marks == 1){
      tmp += dest<<((typenum >> 1) + 1)
    } else tmp += src<<((typenum >> 1) + 1)
    tmp |= (message_type << 1)
    tmp |= marks
    tmp
  }
}