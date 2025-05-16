package org.engine.renderer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

public class Shader {

    private int shaderProgramID;

    private String vertexShaderSource;
    private String fragmentShaderSource;
    private String filepath;

    public Shader(String filepath) {
        this.filepath = filepath;
        try {
            String source = new String(Files.readAllBytes(Paths.get(filepath)));
            String[] splitString = source.split("(#type)( )+([a-zA-Z]+)");

            // Find first pattern after #type
            int index = source.indexOf("#type") + 6;
            int eolIndex = source.indexOf("\n", index);
            String firstPattern = source.substring(index, eolIndex).trim();

            // Find second pattern
            index = source.indexOf("#type", eolIndex) + 6;
            eolIndex = source.indexOf("\n", index);
            String secondPattern = source.substring(index, eolIndex).trim();

            switch (firstPattern) {
                case "vertex":
                    vertexShaderSource = splitString[1];
                    break;
                case "fragment":
                    fragmentShaderSource = splitString[1];
                    break;
                default:
                    throw new IOException("Unexpected token: '" + firstPattern + "'");
            }

            switch (secondPattern) {
                case "vertex":
                    vertexShaderSource = splitString[2];
                    break;
                case "fragment":
                    fragmentShaderSource = splitString[2];
                    break;
                default:
                    throw new IOException("Unexpected token: '" + secondPattern + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("ERROR: Could not open file for shader: '" + filepath + "'");
        }
    }

    public void compile() {
        // -------------------------
        // Compile and link shaders
        // -------------------------

        int vertexID, fragmentID;

        vertexID = glCreateShader(GL_VERTEX_SHADER);
        // Pass shader source to gpu
        glShaderSource(vertexID, vertexShaderSource);
        glCompileShader(vertexID);

        // Check for errors in compilation
        int vertexSuccess = glGetShaderi(vertexID, GL_COMPILE_STATUS);
        if (vertexSuccess == GL_FALSE) {
            int len = glGetShaderi(vertexID, GL_INFO_LOG_LENGTH);
            System.out.println("ERROR: '" + filepath + "'\n\tVertex shader compilation failed.");
            System.out.println(glGetShaderInfoLog(vertexID, len));
            throw new IllegalStateException("Vertex shader did not compile correctly.");
        }

        // Load and compile fragment shader
        fragmentID = glCreateShader(GL_FRAGMENT_SHADER);
        // Pass shader source to gpu
        glShaderSource(fragmentID, fragmentShaderSource);
        glCompileShader(fragmentID);

        // Check for errors in compilation
        int fragmentSuccess = glGetShaderi(fragmentID, GL_COMPILE_STATUS);
        if (fragmentSuccess == GL_FALSE) {
            int len = glGetShaderi(fragmentID, GL_INFO_LOG_LENGTH);
            System.out.println("ERROR: '" + filepath + "'\n\tFragment shader compilation failed.");
            System.out.println(glGetShaderInfoLog(fragmentID, len));
            throw new IllegalStateException("Fragment shader did not compile correctly.");
        }

        // Link shaders and check for errors
        shaderProgramID = glCreateProgram();
        glAttachShader(shaderProgramID, vertexID);
        glAttachShader(shaderProgramID, fragmentID);
        glLinkProgram(shaderProgramID);

        int linkSuccess = glGetProgrami(shaderProgramID, GL_LINK_STATUS);
        if (linkSuccess == GL_FALSE) {
            int len = glGetProgrami(shaderProgramID, GL_INFO_LOG_LENGTH);
            System.out.println("ERROR: '" + filepath + "'\n\tLinking shaders failed.");
            System.out.println(glGetProgramInfoLog(shaderProgramID, len));
            throw new IllegalStateException("Shaders did not link correctly.");
        }
    }

    public void use() {
        // Bind shader program
        glUseProgram(shaderProgramID);
    }

    public void detach() {
        glUseProgram(0);
    }

    public void uploadMat4f(String varName, Matrix4f mat4) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        FloatBuffer matBuffer = BufferUtils.createFloatBuffer(16);
        mat4.get(matBuffer);
        glUniformMatrix4fv(varLocation, false, matBuffer);
    }
}
