

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.text.AttributeSet.ColorAttribute;

public class JPong
{
	public static void main(String arg[])
	{
		JPongFrame f = new JPongFrame();
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		//f.setSize(JPongFrame.WIDTH,JPongFrame.HEIGHT);
		f.setExtendedState(JFrame.MAXIMIZED_BOTH);
		f.setUndecorated(true);
		f.setVisible(true);
		
		f.setup();
		f.draw();		
	}
}

class JPongFrame extends JFrame implements KeyListener
{
	//Constants
		public static int WIDTH, HEIGHT, WINSCORE=10;
	//buffer for drawing off screen
		private Image raster;
	//graphics for the buffer
		private Graphics rasterGraphics;	
	//other
		public static Player rightPlayer, leftPlayer;
		public boolean PAUSE=false;
	/**
	 * Call after the frame is visible but before the frame is drawn
	 */
	public void setup()
	{
		WIDTH = getWidth();
		HEIGHT = getHeight();
		//graphics
			raster = this.createImage(WIDTH, HEIGHT);
			rasterGraphics = raster.getGraphics();
		//players		
			leftPlayer = new ComputerPlayer(40, 1, Color.yellow);
			rightPlayer = new ComputerPlayer(getWidth()-50, -1, Color.green);
		//other
			this.addKeyListener(this);
		
	}
	
	/**
	 * This is the workhorse of the program. This is where the main loop for graphics is done
	 */
	public void draw()
	{
		//create and add the balls to an array to use later
			ArrayList<MultiballWithCollision> multiballs = new ArrayList<MultiballWithCollision>();
			multiballs.add(new MultiballWithCollision(100,100,2.0f,2.0f,Color.red));
		int loop=0;
		boolean game = true;
		while(game)
		{
			//get the start time of the loop to use later
				long time = System.currentTimeMillis();
			if (!PAUSE)
			{
				loop++;
				if (loop > multiballs.size()*1000)
				{
					loop=0;
					multiballs.add(new MultiballWithCollision(getWidth()/2,getHeight()/2,(float)Math.random()*3,(float)Math.random()*3,
							new Color((int)(Math.random()*255),(int)(Math.random()*255),(int)(Math.random()*255))));
				}				
				
				//draw and move everything
					DrawBackground(rasterGraphics);
				//Win Conditions
					if (leftPlayer.Points > WINSCORE)
					{
						rasterGraphics.setColor(Color.white);
						rasterGraphics.setFont(new Font("Arial",Font.PLAIN,46));
						rasterGraphics.drawString("Left Player Wins",200,200);
						game=false;
					}
					else if (rightPlayer.Points > WINSCORE)
					{
						rasterGraphics.setColor(Color.white);
						rasterGraphics.setFont(new Font("Arial",Font.PLAIN,46));
						rasterGraphics.drawString("Right Player Wins",200,200);
						game=false;
					}
				//Balls
					for(MultiballWithCollision b : multiballs)
					{
						b.MoveBall(multiballs,loop);
						b.DrawBall(rasterGraphics);							
					}
				//Paddles
					rasterGraphics.setColor(Color.white);
					leftPlayer.MovePaddle(multiballs);
					leftPlayer.DrawPaddle(rasterGraphics);
					rasterGraphics.setColor(Color.white);
					rasterGraphics.setFont(new Font("Arial",Font.PLAIN,16));
					rasterGraphics.drawString(""+leftPlayer.Points,50,50);
					
					rightPlayer.MovePaddle(multiballs);
					rightPlayer.DrawPaddle(rasterGraphics);
					rasterGraphics.drawString(""+rightPlayer.Points,getWidth()-50,50);
				//draw the scene from the buffered raster
					getGraphics().drawImage(raster,0,0,getWidth(), getHeight(),null);
			}//end of if pause 
			long deltaTime = System.currentTimeMillis() - time;
			try{Thread.sleep(10-deltaTime);}catch(Exception e){}
			
		}//end of game loop
		try{Thread.sleep(5000);}catch(Exception e){}
	}
	
	
	private void DrawBackground(Graphics g) 
	{
		g.setColor(Color.black);
		g.fillRect(0,0,getWidth(), getHeight());
	}	
	public void keyPressed(KeyEvent k)
	{
		if (KeyEvent.VK_ESCAPE == k.getKeyCode())
			System.exit(0);
		if (KeyEvent.VK_SPACE == k.getKeyCode())
			PAUSE = !PAUSE;
	}
	public void keyReleased(KeyEvent k){}
	public void keyTyped(KeyEvent k){}
}
class MultiballWithCollision
{
	public Color C;
	public int radius;
	public Vector2D position, Velocity;
	public Vector2D previousPosition1, previousPosition2;//supports trails
	//These variables are how much we are going to change the current X and Y per loop
	public float speed;
	
	public MultiballWithCollision(int x, int y, float xv, float yv, Color c)
	{
		speed = 1.5f;
		position = new Vector2D();
		previousPosition1 = new Vector2D();
		previousPosition2 = new Vector2D();
		Velocity = new Vector2D();
		position.set(x,y);
		Velocity.set(xv, yv);
		C=c;
		radius=10+(int)(Math.random()*20);
	}
	
	private static double Distance(Vector2D position2, Vector2D position3) 
	{
		return Math.sqrt(Math.pow(position2.getX()-position3.getX(),2) + Math.pow(position2.getY()-position3.getY(),2));
	}
	public void MoveBall(ArrayList<MultiballWithCollision> balls, int loop)
	{
		//Check for collision with another ball
			for(MultiballWithCollision b : balls)
			{
				if (b != this && this.colliding(b))
				{
					this.resolveCollision(b);
				}
			}
			if (JPongFrame.leftPlayer.checkCollision(this))
			{
				this.Velocity.setX(this.Velocity.getX()*-1);
			}
			if (JPongFrame.rightPlayer.checkCollision(this))
			{
				this.Velocity.setX(this.Velocity.getX()*-1);
			}
		//Check boundaries for the ball
			if (position.getX() > JPongFrame.WIDTH-25)
			{
				Velocity.setX(Velocity.getX() * -1);
				JPongFrame.leftPlayer.Points++;
			}
			else if (position.getX() < 20)
			{
				Velocity.setX(Velocity.getX() * -1);
				JPongFrame.rightPlayer.Points++;
			}
			if (position.getY() > JPongFrame.HEIGHT-25)
			{
				Velocity.setY(Velocity.getY() * -1);
			}
			else if (position.getY() < 40)
			{
				Velocity.setY(Velocity.getY() * -1);
			}

		//update the ball's current location
			if (loop%10 == 1)//we want some time before we grab the next trail
			{
				previousPosition2 = previousPosition1;
				previousPosition1 = position;
			}
			position = position.add( Velocity.multiply(speed) );
	}
	public void DrawBall(Graphics g)
	{
		g.setColor(C);
		drawCircle(g,(int)position.getX(),(int)position.getY(), radius);
		//Draw the trail
		drawCircle(g,(int)previousPosition1.getX(),(int)previousPosition1.getY(), (int)(radius*0.5));	
		drawCircle(g,(int)previousPosition2.getX(),(int)previousPosition2.getY(), (int)(radius*0.3));	
	}
	
	public boolean colliding(MultiballWithCollision ball)
	{
	    float xd = position.getX() - ball.position.getX();
	    float yd = position.getY() - ball.position.getY();

	    float sumRadius = radius + ball.radius;
	    float sqrRadius = sumRadius * sumRadius;

	    float distSqr = (xd * xd) + (yd * yd);

	    if (distSqr <= sqrRadius)
	    {
	    	return true;
	    }

	    return false;
	}
	//What happens if they collide
	public void resolveCollision(MultiballWithCollision ball)
	{
	    // get the mtd
	    Vector2D delta = position.subtract(ball.position);	   
	    float d = delta.getLength();
	    // minimum translation distance to push balls apart after intersecting
	    Vector2D mtd = delta.multiply(((radius + ball.radius)-d)/d); 

	    // resolve intersection --
	    // inverse mass quantities
	    float im1 = 1 / (1+(radius-20)/50); //If the balls have different masses you can use this
	    float im2 = 1 / (1+(radius-20)/50); 

	    // push-pull them apart based off their mass
	    position = position.add(mtd.multiply(im1 / (im1 + im2)));
	    ball.position = ball.position.subtract(mtd.multiply(im2 / (im1 + im2)));

	    // impact speed
	    Vector2D v = (this.Velocity.subtract(ball.Velocity));
	    float vn = v.dot(mtd.normalize());

	    // sphere intersecting but moving away from each other already
	    if (vn > 0.0f) 
	    	return;

	    // collision impulse
	    float i = (-(1.0f + 1.0f) * vn) / (im1 + im2);
	    Vector2D impulse = mtd.multiply(i);

	    // change in momentum
	    this.Velocity = this.Velocity.add(impulse.multiply(im1));
	    ball.Velocity = ball.Velocity.subtract(impulse.multiply(im2));

	}
	public void drawCircle(Graphics cg, int xCenter, int yCenter, int r) 
	{
		cg.fillOval(xCenter-r, yCenter-r, 2*r, 2*r);
	}
}

class Heap<E extends Comparable>
{

	private java.util.ArrayList<E> list = new java.util.ArrayList<E>();

	/** Create a default heap */
	public Heap()
	{
	}

	/** Create a heap from an array of objects */
	public Heap(E[] objects)
	{
		for (int i = 0; i < objects.length; i++)
			add(objects[i]);
	}

	/** Add a new object into the heap */
	public void add(E newObject)
	{
		list.add(newObject); // Append to the heap
		int currentIndex = list.size() - 1; // The index of the last node

		while (currentIndex > 0)
		{
			int parentIndex = (currentIndex - 1) / 2;
			// Swap if the current object is greater than its parent
			if (list.get(currentIndex).compareTo(list.get(parentIndex)) > 0)
			{
				E temp = list.get(currentIndex);
				list.set(currentIndex, list.get(parentIndex));
				list.set(parentIndex, temp);
			} else
				break; // the tree is a heap now

			currentIndex = parentIndex;
		}
	
	}

	public String toString()
	{
		String s = "[";
		for (int i = 0; i < list.size() - 1; i++)
			s += list.get(i) + ", ";
		s += list.get(list.size() ) + "]";
		return s;
	}

	/** Remove the root from the heap */
	public E remove()
	{
		if (list.size() == 0)
			return null;

		E removedObject = list.get(0);
		list.set(0, list.get(list.size() - 1));
		list.remove(list.size() - 1);

		int currentIndex = 0;
		while (currentIndex < list.size())
		{
			int leftChildIndex = 2 * currentIndex + 1;
			int rightChildIndex = 2 * currentIndex + 2;

			// Find the maximum between two children
			if (leftChildIndex >= list.size())
				break; // The tree is a heap
			int maxIndex = leftChildIndex;
			if (rightChildIndex < list.size())
			{
				if (list.get(maxIndex).compareTo(list.get(rightChildIndex)) < 0)
				{
					maxIndex = rightChildIndex;
				}
			}

			// Swap if the current node is less than the maximum
			if (list.get(currentIndex).compareTo(list.get(maxIndex)) < 0)
			{
				E temp = list.get(maxIndex);
				list.set(maxIndex, list.get(currentIndex));
				list.set(currentIndex, temp);
				currentIndex = maxIndex;
			} else
				break; // The tree is a heap
		}

		return removedObject;
	}

	/** Get the number of nodes in the tree */
	public int getSize()
	{
		return list.size();
	}
}

abstract class Player
{	
	public final int direction;
	private Color color;
	int Points;
	int YLocation;
	int XLocation;
	public boolean UP, DOWN;
	public static final int HEIGHT=100;
	abstract void MovePaddle(ArrayList<MultiballWithCollision> balls); 
	public Player(int direction, Color c)
	{
		this.direction = direction;
		color = c;
	}
	public void DrawPaddle(Graphics g) 
	{
		g.setColor(color);
		g.fillRect(XLocation, YLocation, 10, HEIGHT);		
	}
	public boolean checkCollision(MultiballWithCollision ball)
	{
		if (Math.abs(ball.position.getX() - XLocation) < 10 &&
				ball.position.getY() > YLocation &&
				ball.position.getY() < YLocation+HEIGHT &&
				ball.Velocity.getX()*direction < 0) //make sure it is going in the right direction
		{
			return true;		
		}
		return false;
	}
	
}
class ComputerPlayer extends Player
{
	//difficulty levels
	public static int Speed = 5;
	public ComputerPlayer(int xLocation, int Direction, Color c)
	{
		super(Direction,c);
		this.XLocation = xLocation;
	}
	public void MovePaddle(ArrayList<MultiballWithCollision> balls) 
	{
		float least=Speed*100;//tracking
		float yLoc = JPongFrame.HEIGHT/2;
		for (MultiballWithCollision ball : balls)
		{
			if (ball.Velocity.getX() * direction > 0)
				continue; //don't worry about his ball since it is going away from me
			if (Math.abs(ball.position.getX() - XLocation) < least)
			{
				least = Math.abs(ball.position.getX() - XLocation);
				yLoc = ball.position.getY();
			}
		}		
		if (yLoc > YLocation+HEIGHT/2)
			YLocation += Speed;
		else if (yLoc < YLocation+HEIGHT/2 - Speed)
			YLocation -= Speed;		
	}
}

//A class that takes care of ball position and speed vectors. Author Zaheer Ahmed
//This is a rather generic Vector2D implementation I've coded these before I was
//just too lazy to write my own so I borrowed one. This one has basic vector
//operations - These are covered in linear algebra or physics classes
class Vector2D 
{
    private float x;
    private float y;

    public Vector2D() 
    {
        this.setX(0);
        this.setY(0);
    }

    public Vector2D(float x, float y) 
    {
        this.setX(x);
        this.setY(y);
    }

    public void set(float x, float y) 
    {
        this.setX(x);
        this.setY(y);
    }

    public void setX(float x) 
    {
        this.x = x;
    }

    public void setY(float y) 
    {
        this.y = y;
    }

    public float getX() 
    {
        return x;
    }

    public float getY() 
    {    	
        return y;
    }

    //Specialty method used during calculations of ball to ball collisions.
    public float dot(Vector2D v2) 
    {
    	float result = 0.0f;
        result = this.getX() * v2.getX() + this.getY() * v2.getY();
        return result;
    }

    public float getLength() 
    {
        return (float) Math.sqrt(getX() * getX() + getY() * getY());
    }

    public Vector2D add(Vector2D v2) 
    {
        Vector2D result = new Vector2D();
        result.setX(getX() + v2.getX());
        result.setY(getY() + v2.getY());
        return result;
    }

    public Vector2D subtract(Vector2D v2) 
    {
        Vector2D result = new Vector2D();
        result.setX(this.getX() - v2.getX());
        result.setY(this.getY() - v2.getY());
        return result;
    }

    public Vector2D multiply(float scaleFactor) 
    {
        Vector2D result = new Vector2D();
        result.setX(this.getX() * scaleFactor);
        result.setY(this.getY() * scaleFactor);
        return result;
    }

    //Specialty method used during calculations of ball to ball collisions.
    public Vector2D normalize() 
    {
    	float length = getLength();
        if (length != 0.0f) 
        {
            this.setX(this.getX() / length);
            this.setY(this.getY() / length);
        } 
        else 
        {
            this.setX(0.0f);
            this.setY(0.0f);
        }
        return this;
    }
    public String toString()
    {
    	return "("+x+", "+y+")";
    }
}